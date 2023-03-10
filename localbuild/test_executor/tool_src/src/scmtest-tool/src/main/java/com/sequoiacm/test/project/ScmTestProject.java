package com.sequoiacm.test.project;

import com.sequoiacm.test.common.BashUtil;
import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.ExecResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScmTestProject {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestProject.class);

    private String name;
    private String pomPath;
    private String compileTargetPath;
    private String jarPath;
    private List<CompileVariable> compileVariableList = new ArrayList<>();
    private List<TestNgXml> testNgXmlList = new ArrayList<>();

    public void doCompile() throws IOException {
        logger.info("Compiling the {} test project...", name);
        String scmVersion = getScmVersion();
        copyDependencies(scmVersion);

        StringBuilder mvnCommand = new StringBuilder("mvn clean install");
        mvnCommand.append(" -f ").append(this.pomPath);
        mvnCommand.append(" -DoutputDirectory=").append(compileTargetPath);
        for (CompileVariable compileVariable : compileVariableList) {
            String variableValue = compileVariable.getVariableValue();
            variableValue = variableValue.replace("3.2.1", scmVersion);
            variableValue = variableValue.replace("(", "\\(").replace(")", "\\)");
            compileVariable.setVariableValue(variableValue);
            mvnCommand.append(" -D").append(compileVariable.getVariableName()).append("=")
                    .append(compileVariable.getVariableValue());
        }
        mvnCommand.append(" -DskipTests=true");

        ExecResult execResult = BashUtil.exec(mvnCommand.toString());
        if (execResult.getExitCode() != 0) {
            throw new IOException(
                    "Failed to compile the test project:" + name + ", detail: " + execResult);
        }
        logger.info("Compile the {} test project success, pom path:{}", name, pomPath);
    }

    private void copyDependencies(String scmVersion) throws IOException {
        // 拷贝驱动 jar 包到测试用例依赖目录下
        // 兼容项目未编译的情况，如果项目未编译，则需要手动拷贝驱动包到 testcase-base/lib/ 目录下
        File testcaseLib = new File(
                LocalPathConfig.BASE_PATH + "../../testcases/v2.0/testcase-base/lib/");

        String driverJarPath = LocalPathConfig.BASE_PATH
                + "../../driver/java/target/sequoiacm-driver-" + scmVersion
                + "-release/sequoiacm-driver-" + scmVersion;
        File driverJarDir = new File(driverJarPath);
        if (driverJarDir.exists()) {
            for (File dependentFile : driverJarDir.listFiles()) {
                if (!dependentFile.isDirectory()) {
                    FileUtils.copyFileToDirectory(dependentFile, testcaseLib);
                }
            }
        }
    }

    private String getScmVersion() throws IOException {
        try {
            String versionInfo = CommonUtil
                    .readContentFromLocalFile(
                            LocalPathConfig.BASE_PATH + "../../script/dev/version");
            // OldVersion=3.2.0
            // NewVersion=3.2.1
            String matchStr = "NewVersion=";
            return versionInfo.substring(versionInfo.lastIndexOf(matchStr) + matchStr.length());
        }
        catch (Exception e) {
            throw new IOException("Failed to get scm version", e);
        }
    }

    public String getName() {
        return name;
    }

    public String getJarPath() throws IOException {
        if (jarPath == null) {
            File targetDir = new File(compileTargetPath);
            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.contains(this.name) && fileName.endsWith(".jar")) {
                        jarPath = file.getAbsolutePath();
                        break;
                    }
                }
            }

            if (jarPath == null) {
                throw new IOException(
                        "Jar file of test project not found, please compile the test project: "
                                + this.name);
            }
        }
        return jarPath;
    }

    public List<CompileVariable> getCompileVariableList() {
        return compileVariableList;
    }

    public List<TestNgXml> getTestNgXmlByName(List<String> testNgNameList) {
        List<TestNgXml> result = new ArrayList<>();
        for (TestNgXml testNgXml : testNgXmlList) {
            if (testNgNameList.contains(testNgXml.getName())) {
                result.add(testNgXml);
            }
        }
        return result;
    }

    public List<TestNgXml> getTestNgXmlList() {
        return testNgXmlList;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPomPath(String pomPath) {
        this.pomPath = pomPath;
    }

    public void setCompileTargetPath(String compileTargetPath) {
        this.compileTargetPath = compileTargetPath;
    }

}
