package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.*;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.TestTaskInfo;
import com.sequoiacm.test.module.Worker;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ScmTestExecutorFactory {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestExecutorFactory.class);
    private static Map<Worker, ScmTestExecutor> testExecutorMap = new HashMap<>();
    private static volatile ScmTestExecutorFactory INSTANCE;

    private ScmTestExecutorFactory() {

    }

    public static ScmTestExecutorFactory getInstance() {
        if (INSTANCE == null) {
            synchronized (ScmTestExecutorFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScmTestExecutorFactory();
                    ShutdownHookMgr.getInstance().addHook(new ShutdownHook(10) {
                        @Override
                        public void onShutdown() {
                            checkAndAbortAllExecutor();
                        }
                    });
                }
            }
        }

        return INSTANCE;
    }

    public ScmTestExecutor createExecutor(TestTaskInfo taskInfo) throws IOException {
        Worker worker = taskInfo.getWorker();
        ScmTestExecutor testExecutor = testExecutorMap.get(worker);
        if (testExecutor == null) {
            testExecutor = worker.isLocalWorker() ? new ScmTestLocalExecutor(taskInfo)
                    : new ScmTestRemoteExecutor(taskInfo);
            testExecutorMap.put(worker, testExecutor);
        }
        else {
            testExecutor.updateTaskInfo(taskInfo);
        }
        return testExecutor;
    }

    private static void checkAndAbortAllExecutor() {
        for (Map.Entry<Worker, ScmTestExecutor> entry : testExecutorMap.entrySet()) {
            Worker worker = entry.getKey();
            ScmTestExecutor testExecutor = entry.getValue();
            if (testExecutor.getTaskInfo().isDone()) {
                continue;
            }

            String execCommand = testExecutor.getTaskInfo().getExecCommand();
            if (worker.isLocalWorker()) {
                BashUtil.searchProcessAndKill(execCommand);
                continue;
            }

            Ssh ssh = null;
            try {
                ssh = SshMgr.getInstance().getSsh(worker.getHostInfo());
                String psCommand = "ps -eo pid,cmd | grep -w \""
                        + StringUtil.subStringBefore(execCommand, " >")
                        + "\" | grep -v -e grep -e bash";
                ExecResult psResult = ssh.exec(psCommand, null);

                String processDes = psResult.getStdOut().trim();
                String pidStr = StringUtil.subStringBefore(processDes, " ");
                if (pidStr.length() > 0) {
                    int pid = Integer.parseInt(pidStr);
                    logger.info("Killing the remote testcase program, worker={}, pid={}",
                            worker.getName(), pid);

                    ExecResult execResult = ssh.sudoExec("kill -9 " + pid);
                    if (execResult.getExitCode() == 0) {
                        logger.info("Kill the remote test program success, worker={}, pid={}",
                                worker.getName(), pid);
                    }
                    else {
                        throw new IOException(execResult.getStdErr());
                    }
                }
            }
            catch (Exception e) {
                logger.error(
                        "Failed to check and kill the remote testcase program, worker={}, cause by: {}",
                        worker.getName(), e.getMessage(), e);
            }
            finally {
                CommonUtil.closeResource(ssh);
            }
        }
    }
}
