package com.sequoiacm.test.module;

import java.io.File;
import java.io.IOException;

import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.project.ScmTestProject;
import com.sequoiacm.test.project.ScmTestProjectMgr;
import com.sequoiacm.test.project.TestNgXml;

public class TestTaskInfo {

    private Worker worker;
    private TestNgXml testNgXml;
    private String execCommand;
    private String consoleOutPath;
    private String errorDetailPath;
    private String reportPath;
    private boolean isDone;

    public TestTaskInfo(Worker worker, TestNgXml testNgXml) throws IOException {
        this.worker = worker;
        this.testNgXml = testNgXml;
        this.execCommand = initExecCommand();
    }

    public Worker getWorker() {
        return worker;
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

    public String getErrorDetailPath() {
        return errorDetailPath;
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

        WorkPath workPath = worker.getWorkPath();

        String jvmOption = "-DbasePath=" + workPath.getBasePath();
        reportPath = workPath.getTestOutputPath() + testProject.getName();
        consoleOutPath = workPath.getConsoleOutPath() + testProject.getName() + "-console.out";
        errorDetailPath = workPath.getTmpPath() + "error.detail";
        String proJarPath = workPath.getJarPath() + new File(testProject.getJarPath()).getName();
        String testListenerPath = workPath.getJarPath()
                + new File(LocalPathConfig.getListenerJarPath()).getName();
        String testNgXmlPath = workPath.getTmpPath() + new File(testNgXml.getPath()).getName();
        String jarSeparator = worker.isLocalWorker() ? File.pathSeparator : ":";
        return "java " + jvmOption + " -cp " + proJarPath + jarSeparator + testListenerPath
                + " org.testng.TestNG " + testNgXmlPath
                + " -listener com.sequoiacm.test.listener.ScmTestNgListener -d " + reportPath
                + " > " + consoleOutPath + " 2>&1";
    }

    @Override
    public String toString() {
        return "TestTaskInfo{ +" + "host=" + worker.getName() + "testngXml=" + testNgXml + "}";
    }
}
