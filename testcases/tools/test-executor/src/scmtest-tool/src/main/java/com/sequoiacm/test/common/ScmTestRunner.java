package com.sequoiacm.test.common;

import com.sequoiacm.test.config.ScmTestToolProps;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.exec.*;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.module.TestTaskInfoGroup;
import com.sequoiacm.test.project.TestNgXml;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScmTestRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestRunner.class);

    private List<TestTaskInfoGroup> taskGroupList;
    private ExecutorService cachedThreadPool;

    public ScmTestRunner(List<TestTaskInfoGroup> taskGroupList) {
        this.taskGroupList = taskGroupList;
        List<HostInfo> hostInfoList = ScmTestToolProps.getInstance().getWorkers();
        cachedThreadPool = Executors.newFixedThreadPool(hostInfoList.size());
    }

    public void runAndCollectReport() throws Exception {
        ScmTestExecutorFactory executorFactory = ScmTestExecutorFactory.getInstance();
        for (int i = 0; i < taskGroupList.size(); i++) {
            TestTaskInfoGroup taskGroup = taskGroupList.get(i);
            logger.info("({}/{}) Running test: {}", i + 1, taskGroupList.size(),
                    taskGroup.getXmlName());

            List<ScmTestExecutor> executorList = new ArrayList<>();
            for (TestTaskInfo taskInfo : taskGroup.getTaskInfoList()) {
                ScmTestExecutor testExecutor = executorFactory.createExecutor(taskInfo);
                executorList.add(testExecutor);
            }

            List<ScmTestProgress> psList = new ArrayList<>();
            for (ScmTestExecutor testExecutor : executorList) {
                ScmTestProgress progress = runTest(testExecutor);
                psList.add(progress);
            }

            // (1 / x) : testng
            String currentTestStep = "(" + (i + 1) + "/" + taskGroupList.size() + ") "
                    + taskGroup.getXmlName();
            ScmTestProgressPrinter progressPrinter = new ScmTestProgressPrinter(psList,
                    currentTestStep);
            progressPrinter.printUntilAllTestCompleted();
            for (TestTaskInfo taskInfo : taskGroup.getTaskInfoList()) {
                taskInfo.setDone(true);
            }

            collectTestReport(taskGroup.getTaskInfoList());
            if (!isTaskGroupCompleted(psList, taskGroup.getTaskInfoList())) {
                break;
            }
        }
    }

    private ScmTestProgress runTest(ScmTestExecutor testExecutor) {
        Future<ExecResult> future = cachedThreadPool.submit(testExecutor);

        HostInfo hostInfo = testExecutor.getTaskInfo().getHostInfo();
        if (hostInfo.isLocalHost()) {
            return new ScmTestLocalProgress(hostInfo, future);
        }
        return new ScmTestRemoteProgress(hostInfo, future);
    }

    private boolean isTaskGroupCompleted(List<ScmTestProgress> psList,
            List<TestTaskInfo> taskInfoList) {
        boolean isComplete = true;
        for (int i = 0; i < psList.size(); i++) {
            ScmTestProgress testProgress = psList.get(i);
            TestTaskInfo taskInfo = taskInfoList.get(i);

            if (!testProgress.isExecSuccess()) {
                isComplete = false;
                logger.error("The test run on {} has failed, you can run it again by command: {}",
                        taskInfo.getHostInfo().getHostname(), taskInfo.getExecCommand());
            }
            else if (!testProgress.isPassAllTestcase()) {
                isComplete = false;
                logger.error(
                        "The test run on {} did not pass all testcase, you can run it again by command: {}",
                        taskInfo.getHostInfo().getHostname(), taskInfo.getExecCommand());
            }
        }
        return isComplete;
    }

    public void collectTestReport(List<TestTaskInfo> taskInfoList) {
        for (TestTaskInfo taskInfo : taskInfoList) {
            TestNgXml testNgXml = taskInfo.getTestNgXml();
            HostInfo hostInfo = taskInfo.getHostInfo();

            if (!hostInfo.isLocalHost()) {
                logger.debug("Collecting test report and console output file, remoteHost={}",
                        hostInfo.getHostname());
                Ssh ssh = null;
                try {
                    ssh = SshMgr.getInstance().getSsh(hostInfo);

                    String localTestOutputPath = LocalPathConfig.TEST_OUTPUT_PATH
                            + hostInfo.getHostname() + File.separator + testNgXml.getProject()
                            + File.separator + testNgXml.getName();
                    CommonUtil.cleanOrInitDir(localTestOutputPath);
                    ssh.scpFolderFrom(localTestOutputPath, taskInfo.getReportPath());

                    String localConsoleOutPath = LocalPathConfig.OUTPUT_PATH
                            + hostInfo.getHostname() + File.separator + testNgXml.getProject()
                            + File.separator + testNgXml.getName() + "-console.out";
                    ssh.scpFileFrom(localConsoleOutPath, taskInfo.getConsoleOutPath());
                    logger.debug(
                            "Collect test report and console output file success, remoteHost={}",
                            hostInfo.getHostname());
                }
                catch (IOException e) {
                    logger.warn("Failed to collect test report and console output file, remoteHost="
                            + hostInfo.getHostname(), e);
                }
                finally {
                    CommonUtil.closeResource(ssh);
                }
            }
        }
    }

    public void stop() {
        if (cachedThreadPool != null) {
            cachedThreadPool.shutdown();
        }
    }
}
