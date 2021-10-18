package com.sequoiacm.test.project;

import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.module.TestTaskInfo;
import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.*;

public class ScmTestNgXmlRefactor {

    private List<HostInfo> hostList = null;
    private TestNgXml testNgXml;

    private Document xmlDocument;
    private Element rootElement;
    private List<Element> testElements;
    private Element firstTest;

    private Document[] documents;

    public ScmTestNgXmlRefactor(TestNgXml testNgXml) throws IOException {
        this.testNgXml = testNgXml;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setEntityResolver(new NoOpEntityResolver());
            this.xmlDocument = saxBuilder.build(new File(testNgXml.getPath()));
            this.rootElement = xmlDocument.getRootElement();
            this.testElements = rootElement.getChildren("test");
            this.firstTest = testElements.get(0);
        }
        catch (Exception e) {
            throw new IOException(
                    "Failed to create the testng xml refactor, cause by:" + e.getMessage(), e);
        }
    }

    public void resetParameters(Map<String, String> paramMap) {
        Map<String, String> newParameter = new HashMap<>();

        // 将 xml 中原有的 parameter 存入 Map
        List<Element> parameterList = rootElement.getChildren("parameter");
        for (Element param : parameterList) {
            String parameterName = param.getAttributeValue("name");
            String parameterValue = param.getAttributeValue("value");
            newParameter.put(parameterName, parameterValue);
        }

        // 将用户指定的 parameter 存入 Map
        for (Map.Entry<String, String> param : paramMap.entrySet()) {
            if (param.getValue() != null && !param.getValue().equals("")) {
                newParameter.put(param.getKey(), param.getValue());
            }
        }

        // 清空 parameter，并重新写入
        rootElement.removeChildren("parameter");
        for (Map.Entry<String, String> param : newParameter.entrySet()) {
            Element parameter = new Element("parameter");
            parameter.setAttribute("name", param.getKey());
            parameter.setAttribute("value", param.getValue());
            rootElement.addContent(parameter);
        }
    }

    public void cleanPackagesAndClasses() {
        if (firstTest.getChild("packages") != null) {
            firstTest.getChild("packages").removeContent();
        }
        if (firstTest.getChild("classes") != null) {
            firstTest.getChild("classes").removeContent();
        }
    }

    public void resetPackages(List<String> packageList) {
        Element packages = firstTest.getChild("packages");
        if (packages == null) {
            packages = new Element("packages");
            firstTest.addContent(packages);
        }

        for (String packageName : packageList) {
            Element childPackage = new Element("package");
            childPackage.setAttribute("name", packageName);
            packages.addContent(childPackage);
        }
    }

    public void resetClasses(List<String> classList) {
        Element classes = firstTest.getChild("classes");
        if (classes == null) {
            classes = new Element("classes");
            firstTest.addContent(classes);
        }

        for (String className : classList) {
            Element childClass = new Element("class");
            childClass.setAttribute("name", className);
            classes.addContent(childClass);
        }
    }

    public void resetThreadCount(String threadCount) {
        rootElement.setAttribute("thread-count", threadCount);
    }

    public void appendGroupFilter(String siteCount) {
        if (firstTest.getChild("method-selectors") != null) {
            Element selector = firstTest.getChild("method-selectors").getChild("method-selector");
            Element script = selector.getChild("script");

            String value = script.getContent(1).getValue();
            String appendValue = "groups.containsKey(\"" + siteCount + "\")||" + value;
            CDATA cdata = new CDATA(appendValue);

            script.removeContent(1);
            script.addContent(1, cdata);
        }
    }

    public ScmTestNgXmlRefactor divide(List<HostInfo> hostList) {
        this.hostList = hostList;
        documents = new Document[hostList.size()];
        for (int i = 0; i < documents.length; i++) {
            documents[i] = xmlDocument.clone();
            Element cloneFirstTest = documents[i].getRootElement().getChild(firstTest.getName());
            // 清空副本中的package和class
            if (firstTest.getChild("packages") != null) {
                cloneFirstTest.getChild("packages").removeContent();
            }
            if (firstTest.getChild("classes") != null) {
                cloneFirstTest.getChild("classes").removeContent();
            }
        }

        // 乱序处理package
        Element packages = firstTest.getChild("packages");
        if (packages != null) {
            List<Element> packageList = packages.getChildren("package");
            // 在 JDOM 实现中，每个元素都有父级链接，将元素添加到新目标之前，必须从原始结构中分离元素
            List<Element> clonePackageList = new ArrayList<>();
            for (Element element : packageList) {
                clonePackageList.add(element.clone());
            }
            Collections.shuffle(clonePackageList);
            // 平均分配到各个主机
            for (int i = 0; i < clonePackageList.size(); i++) {
                Element clonePackage = clonePackageList.get(i);
                documents[i % hostList.size()].getRootElement().getChildren("test").get(0)
                        .getChild("packages").addContent(clonePackage);
            }
        }

        // 乱序处理class
        Element classes = firstTest.getChild("classes");
        if (classes != null) {
            List<Element> classList = classes.getChildren("class");
            List<Element> cloneClassList = new ArrayList<>();
            for (Element element : classList) {
                cloneClassList.add(element.clone());
            }
            Collections.shuffle(cloneClassList);
            // 平均分配到各个主机
            for (int i = 0; i < cloneClassList.size(); i++) {
                Element cloneClass = cloneClassList.get(i);
                documents[i % hostList.size()].getRootElement().getChildren("test").get(0)
                        .getChild("classes").addContent(cloneClass);
            }
        }

        return this;
    }

    public List<TestTaskInfo> doRefactor() throws IOException {
        int packageCount = getElementContentSize(firstTest.getChild("packages"));
        int classCount = getElementContentSize(firstTest.getChild("classes"));
        int numberOfHostNeeded = Math.min(hostList.size(), Math.max(packageCount, classCount));

        XMLOutputter out = new XMLOutputter();
        Format fmt = Format.getPrettyFormat();
        fmt.setEncoding("UTF-8");
        fmt.setIndent("    ");
        out.setFormat(fmt);

        List<TestTaskInfo> taskInfoList = new ArrayList<>();
        for (int i = 0; i < numberOfHostNeeded; i++) {
            try {
                StringBuilder fileName = new StringBuilder(testNgXml.getProject());
                fileName.append("-");
                fileName.append(testNgXml.getName());
                fileName.append("-");
                fileName.append(hostList.get(i).getHostname());
                fileName.append(".xml");

                String xmlPath = LocalPathConfig.TMP_PATH + fileName;
                out.output(documents[i], new FileOutputStream(xmlPath));

                TestNgXml refactorXml = new TestNgXml(testNgXml.getProject(), testNgXml.getName(),
                        xmlPath);
                TestTaskInfo taskInfo = new TestTaskInfo(hostList.get(i), refactorXml);
                taskInfoList.add(taskInfo);
            }
            catch (IOException e) {
                throw new IOException("Failed to output the testng xml file", e);
            }
        }

        return taskInfoList;
    }

    private int getElementContentSize(Element element) {
        return element == null ? 0 : element.getContentSize() / 2;
    }

    private static class NoOpEntityResolver implements EntityResolver {
        // 取消 dtd 验证，内网环境无法联网验证
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new ByteArrayInputStream("".getBytes()));
        }
    }
}
