package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.CommonUtil;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.WorkPath;
import com.sequoiacm.test.module.Worker;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class ScmTestLocalProgress extends ScmTestProgress {

    private static final Logger logger = LoggerFactory.getLogger(ScmTestLocalProgress.class);

    public ScmTestLocalProgress(Worker worker, Future<ExecResult> future) {
        super(worker, future);
    }

    @Override
    protected BSONObject readFromProgressFile() {
        BSONObject testProgress = null;
        try {
            WorkPath workPath = worker.getWorkPath();
            testProgress = CommonUtil.parseJsonFile(workPath.getTestProgressPath());
        }
        catch (Exception e) {
            logger.debug("Failed to update the localhost test progress, cause by:{}",
                    e.getMessage(), e);
        }
        return testProgress;
    }
}
