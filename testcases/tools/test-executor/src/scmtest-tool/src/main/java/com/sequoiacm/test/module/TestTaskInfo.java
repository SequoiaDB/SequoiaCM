package com.sequoiacm.test.module;

import com.sequoiacm.test.common.CommonDefine;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.config.RemotePathConfig;
import com.sequoiacm.test.project.ScmTestProject;
import com.sequoiacm.test.project.ScmTestProjectMgr;
import com.sequoiacm.test.project.TestNgXml;

import java.io.File;
import java.io.IOException;

public class TestTaskInfo {

    private HostInfo hostInfo;
    private TestNgXml testNgXml;
    private String execCommand;
    private String consoleOutPath;
    private String reportPath;
    private boolean isDone;

    public TestTaskInfo(HostInfo hostInfo, TestNgXml testNgXml) throws IOException {
        this.hostInfo = hostInfo;
        this.testNgXml = testNgXml;
        this.execCommand = initExecCommand();
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

    public TestNgXml getTestNgXml() {
        return testNgXml;
    }

    public String getExecCommand() {
        return execCommand;
    }

    public String getConsoleOutPath() {
        return consoleOutPath;
    }

    public String getReportPath() {
        return reportPath;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    private String initExecCommand() throws IOException {
        ScmTestProjectMgr testProjectMgr = ScmTestProjectMgr.getInstance();
        ScmTestProject testProject = testProjectMgr.getTestProject(testNgXml.getProject());

        String workPath;
        String proJarPath = testProject.getJarPath();
        String testListenerPath = LocalPathConfig.getListenerJarPath();
        String testNgXmlPath = testNgXml.getPath();
        if (hostInfo.isLocalHost()) {
            workPath = LocalPathConfig.BASE_PATH;
            reportPath = LocalPathConfig.TEST_OUTPUT_PATH + CommonDefine.LOCALHOST + File.separator
                    + testProject.getName() + File.separator + testNgXml.getName();
            consoleOutPath = LocalPathConfig.OUTPUT_PATH + CommonDefine.LOCALHOST + File.separator
                    + testProject.getName() + File.separator + testNgXml.getName() + "-console.out";
        }
        else {
            workPath = RemotePathConfig.WORK_PATH;
            reportPath = RemotePathConfig.TEST_OUTPUT_PATH + testProject.getName();
            consoleOutPath = RemotePathConfig.OUTPUT_PATH + testProject.getName() + "-console.out";

            proJarPath = RemotePathConfig.JARS_PATH + new File(proJarPath).getName();
            testListenerPath = RemotePathConfig.JARS_PATH + new File(testListenerPath).getName();
            testNgXmlPath = RemotePathConfig.TMP_PATH + new File(testNgXmlPath).getName();
        }

        String jvmOption = "-DbasePath=" + workPath;
        String jarSeparator = hostInfo.isLocalHost() ? File.pathSeparator : ":";
        return "java " + jvmOption + " -cp " + proJarPath + jarSeparator + testListenerPath
                + " org.testng.TestNG " + testNgXmlPath
                + " -listener com.sequoiacm.test.listener.ScmTestNgListener -d " + reportPath
                + " > " + consoleOutPath + " 2>&1";
    }

    @Override
    public String toString() {
        return "TestTaskInfo{ +" +
                "host=" + hostInfo.getHostname() +
                "testngXml=" + testNgXml +
                "}";
    }
}
