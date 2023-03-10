package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.WorkPath;
import com.sequoiacm.test.module.Worker;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

public class ScmTestRemoteProgress extends ScmTestProgress {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestRemoteProgress.class);

    public ScmTestRemoteProgress(Worker worker, Future<ExecResult> future) {
        super(worker, future);
    }

    @Override
    protected BSONObject readFromProgressFile() {
        BSONObject testProgress = null;
        Ssh ssh = null;
        try {
            WorkPath workPath = worker.getWorkPath();
            ssh = SshMgr.getInstance().getSsh(worker.getHostInfo());
            ExecResult execResult = ssh.exec("cat " + workPath.getTestProgressPath(), null);
            if (execResult.getExitCode() != 0) {
                throw new IOException(execResult.getStdErr());
            }
            String progressContent = execResult.getStdOut();
            testProgress = CommonUtil.parseJsonString(progressContent);
        }
        catch (Exception e) {
            logger.debug("Failed to update the test progress, remoteWorker={}, cause by:{}",
                    worker.getName(), e.getMessage());
        }
        finally {
            CommonUtil.closeResource(ssh);
        }
        return testProgress;
    }
}
