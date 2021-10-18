package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.RemotePathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.HostInfo;
import com.sequoiacm.test.ssh.Ssh;
import com.sequoiacm.test.ssh.SshMgr;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

public class ScmTestRemoteProgress extends ScmTestProgress {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestRemoteProgress.class);

    public ScmTestRemoteProgress(HostInfo hostInfo, Future<ExecResult> future) {
        super(hostInfo, future);
    }

    @Override
    protected BSONObject readFromProgressFile() {
        BSONObject testProgress = null;
        Ssh ssh = null;
        try {
            ssh = SshMgr.getInstance().getSsh(hostInfo);
            ExecResult execResult = ssh.exec("cat " + RemotePathConfig.TEST_PROGRESS_PATH, null);
            if (execResult.getExitCode() != 0) {
                throw new IOException(execResult.getStdErr());
            }
            String progressContent = execResult.getStdOut();
            testProgress = CommonUtil.parseJsonString(progressContent);
        }
        catch (Exception e) {
            logger.debug("Failed to update the test progress, remoteHost={}, cause by:{}",
                    hostInfo.getHostname(), e.getMessage());
        }
        finally {
            CommonUtil.closeResource(ssh);
        }
        return testProgress;
    }
}
