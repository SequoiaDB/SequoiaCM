package com.sequoiacm.test.project;

import com.sequoiacm.test.common.BashUtil;
import com.sequoiacm.test.module.ExecResult;
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
        StringBuilder mvnCommand = new StringBuilder("mvn clean install");
        mvnCommand.append(" -f ").append(this.pomPath);
        mvnCommand.append(" -DoutputDirectory=").append(compileTargetPath);
        for (CompileVariable compileVariable : compileVariableList) {
            mvnCommand.append(" -D").append(compileVariable.getVariableName()).append("=")
                    .append(compileVariable.getVariableValue());
        }
        mvnCommand.append(" -Dmaven.test.skip=true");

        ExecResult execResult = BashUtil.exec(mvnCommand.toString());
        if (execResult.getExitCode() != 0) {
            throw new IOException("Failed to compile the test project:" + name + ", cause by: "
                    + execResult.getStdErr() + ", exitCode=" + execResult.getExitCode());
        }
        logger.info("Compile the {} test project success, pom path:{}", name, pomPath);
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

    public List<TestNgXml> getTestNgXmlByTag(String tag) {
        List<TestNgXml> result = new ArrayList<>();
        for (TestNgXml testNgXml : testNgXmlList) {
            if (testNgXml.getTags().contains(tag)) {
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
