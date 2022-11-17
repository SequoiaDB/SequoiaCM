package com.sequoiacm.test.common;

import com.sequoiacm.test.config.ScmTestToolProps;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.exec.*;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.module.TestTaskInfoGroup;
import com.sequoiacm.test.module.Worker;
import com.sequoiacm.test.project.TestNgXml;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScmTestRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestRunner.class);

    private List<TestTaskInfoGroup> taskGroupList;
    private Map<ScmTestProgress, TestTaskInfo> currentGroupMap = new HashMap<>();
    private ExecutorService cachedThreadPool;

    public ScmTestRunner(List<TestTaskInfoGroup> taskGroupList) {
        this.taskGroupList = taskGroupList;
        List<Worker> workers = ScmTestToolProps.getInstance().getWorkers();
        cachedThreadPool = Executors.newFixedThreadPool(workers.size());
    }

    public void runAndCollectReport() throws Exception {
        ScmTestExecutorFactory executorFactory = ScmTestExecutorFactory.getInstance();
        for (int i = 0; i < taskGroupList.size(); i++) {
            TestTaskInfoGroup taskGroup = taskGroupList.get(i);
            logger.info("({}/{}) Running test: {}", i + 1, taskGroupList.size(),
                    taskGroup.getXmlName());

            currentGroupMap.clear();
            List<ScmTestExecutor> executorList = new ArrayList<>();
            for (TestTaskInfo taskInfo : taskGroup.getTaskInfoList()) {
                ScmTestExecutor testExecutor = executorFactory.createExecutor(taskInfo);
                executorList.add(testExecutor);
            }

            List<ScmTestProgress> psList = new ArrayList<>();
            for (ScmTestExecutor testExecutor : executorList) {
                ScmTestProgress progress = runTest(testExecutor);
                psList.add(progress);
                currentGroupMap.put(progress, testExecutor.getTaskInfo());
            }

            // (1 / x) : testng
            String currentTestStep = "(" + (i + 1) + "/" + taskGroupList.size() + ") "
                    + taskGroup.getXmlName();
            ScmTestProgressPrinter progressPrinter = new ScmTestProgressPrinter(psList,
                    currentTestStep, (failedProgresses) -> {
                        List<TestTaskInfo> failTaskInfoList = new ArrayList<>();
                        for (ScmTestProgress failedProgress : failedProgresses) {
                            TestTaskInfo failedTask = currentGroupMap.get(failedProgress);
                            failTaskInfoList.add(failedTask);
                        }
                        collectErrorDetail(failTaskInfoList);
                    });
            progressPrinter.printUntilAllTestCompleted();
            for (TestTaskInfo taskInfo : taskGroup.getTaskInfoList()) {
                taskInfo.setDone(true);
            }

            collectTestReport(taskGroup.getTaskInfoList());
            if (!isCurrentTaskGroupCompleted()) {
                break;
            }
        }
    }
    private ScmTestProgress runTest(ScmTestExecutor testExecutor) {
        Future<ExecResult> future = cachedThreadPool.submit(testExecutor);

        Worker worker = testExecutor.getTaskInfo().getWorker();
        if (worker.isLocalWorker()) {
            return new ScmTestLocalProgress(worker, future);
        }
        return new ScmTestRemoteProgress(worker, future);
    }

    private boolean isCurrentTaskGroupCompleted() {
        boolean isComplete = true;
        for (ScmTestProgress testProgress : currentGroupMap.keySet()) {
            TestTaskInfo taskInfo = currentGroupMap.get(testProgress);

            if (!testProgress.isExecSuccess()) {
                isComplete = false;
                logger.error("The test run on {} has failed, you can run it again by command: {}",
                        taskInfo.getWorker().getName(), taskInfo.getExecCommand());
            }
            else if (!testProgress.isPassAllTestcase()) {
                isComplete = false;
                logger.error(
                        "The test run on {} did not pass all testcase, you can run it again by command: {}",
                        taskInfo.getWorker().getName(), taskInfo.getExecCommand());
            }
        }
        return isComplete;
    }

    private void collectErrorDetail(List<TestTaskInfo> failTaskInfoList) {
        for (TestTaskInfo taskInfo : failTaskInfoList) {
            TestNgXml testNgXml = taskInfo.getTestNgXml();
            Worker worker = taskInfo.getWorker();
            logger.debug("Collecting error detail file, worker={}", worker.getName());
            // e.g.: /opt/scm-test/console-out/192.168.30.76/story/testng
            String consoleOutDirPath = LocalPathConfig.CONSOLE_OUT_PATH + worker.getName()
                    + File.separator + testNgXml.getProject() + File.separator
                    + testNgXml.getName();
            try {
                if (worker.isLocalWorker()) {
                    FileUtils.copyFileToDirectory(new File(taskInfo.getErrorDetailPath()),
                            new File(consoleOutDirPath));
                    return;
                }
                FileUtils.forceMkdir(new File(consoleOutDirPath));
                String errorDetailPath = consoleOutDirPath + File.separator + "error.detail";

                HostInfo hostInfo = taskInfo.getWorker().getHostInfo();
                Ssh ssh = null;
                try {
                    ssh = SshMgr.getInstance().getSsh(hostInfo);
                    ssh.scpFileFrom(errorDetailPath, taskInfo.getErrorDetailPath());
                }
                finally {
                    CommonUtil.closeResource(ssh);
                }
            }
            catch (IOException e) {
                logger.warn("Failed to collect error detail file, worker=" + worker.getName(), e);
            }
            logger.debug("Collect error detail file success, worker={}", worker.getName());
        }
    }

    private void collectTestReport(List<TestTaskInfo> taskInfoList) {
        for (TestTaskInfo taskInfo : taskInfoList) {
            TestNgXml testNgXml = taskInfo.getTestNgXml();
            Worker worker = taskInfo.getWorker();
            logger.debug("Collecting test report and console output file, worker={}",
                    worker.getName());
            // e.g.: /opt/scm-test/console-out/192.168.30.76/story/testng
            String consoleOutDirPath = LocalPathConfig.CONSOLE_OUT_PATH + worker.getName()
                    + File.separator + testNgXml.getProject() + File.separator
                    + testNgXml.getName();
            // e.g.: /opt/scm-test/test-output/192.168.30.76/story/testng
            String targetTestOutputPath = LocalPathConfig.TEST_OUTPUT_PATH + worker.getName()
                    + File.separator + testNgXml.getProject() + File.separator
                    + testNgXml.getName();
            try {
                if (worker.isLocalWorker()) {
                    collectTestReportLocally(consoleOutDirPath, targetTestOutputPath, taskInfo);
                    return;
                }

                FileUtils.forceMkdir(new File(consoleOutDirPath));
                String consoleOutPath = consoleOutDirPath + File.separator + "console.out";
                collectTestReportRemotely(consoleOutPath, targetTestOutputPath, taskInfo);
            }
            catch (IOException e) {
                logger.warn("Failed to collect test report and console output file, worker="
                        + worker.getName(), e);
            }
            logger.debug("Collect test report and console output file success, worker={}",
                    worker.getName());
        }
    }

    private void collectTestReportLocally(String targetConsoleOutPath, String targetTestOutputPath,
            TestTaskInfo taskInfo) throws IOException {
        FileUtils.copyFileToDirectory(new File(taskInfo.getConsoleOutPath()),
                new File(targetConsoleOutPath));
        File reportDir = new File(taskInfo.getReportPath());
        File localTestOutputDir = new File(targetTestOutputPath);
        for (File f : reportDir.listFiles()) {
            if (f.isDirectory()) {
                FileUtils.copyDirectoryToDirectory(f, localTestOutputDir);
                continue;
            }
            FileUtils.copyFileToDirectory(f, localTestOutputDir);
        }
    }

    private void collectTestReportRemotely(String targetConsoleOutPath, String targetTestOutputPath,
            TestTaskInfo taskInfo) throws IOException {
        HostInfo hostInfo = taskInfo.getWorker().getHostInfo();
        Ssh ssh = null;
        try {
            ssh = SshMgr.getInstance().getSsh(hostInfo);
            ssh.scpFileFrom(targetConsoleOutPath, taskInfo.getConsoleOutPath());
            CommonUtil.cleanOrInitDir(targetTestOutputPath);
            ssh.scpFolderFrom(targetTestOutputPath, taskInfo.getReportPath());
        }
        finally {
            CommonUtil.closeResource(ssh);
        }
    }

    public void stop() {
        if (cachedThreadPool != null) {
            cachedThreadPool.shutdown();
        }
    }

    interface ReportCollectCallback {
        void callback(List<ScmTestProgress> failedProgresses);

    }
}
