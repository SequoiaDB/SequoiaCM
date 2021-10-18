package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.config.LocalPathConfig;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.HostInfo;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class ScmTestLocalProgress extends ScmTestProgress {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestLocalProgress.class);

    public ScmTestLocalProgress(HostInfo hostInfo, Future<ExecResult> future) {
        super(hostInfo, future);
    }

    @Override
    protected BSONObject readFromProgressFile() {
        BSONObject testProgress = null;
        try {
            testProgress = CommonUtil.parseJsonFile(LocalPathConfig.LOCAL_TEST_PROGRESS_PATH);
        }
        catch (Exception e) {
            logger.debug("Failed to update the localhost test progress, cause by:{}", e.getMessage(), e);
        }
        return testProgress;
    }
}
