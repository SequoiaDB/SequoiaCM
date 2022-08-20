package com.sequoiacm.infrastructure.lock.curator;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

class CleanCounterContext{
    long deleteCount =0;
    long total = 0;

}
public class CuratorCleanJob extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(CuratorCleanJob.class);
    private CuratorLockFactory curatorLockFactory;
    private long maxResidualTime;
    private int cleanAllCountPeriod;
    private int maxChildNum;
    private CuratorZKCleaner zkCleaner = CuratorZKCleaner.getInstance();
    private int cleanCount = 0;

    public CuratorCleanJob(CuratorLockFactory curatorLockFactory, long maxResidualTime,
                           int maxChildNum, int cleanAllCountPeriod, boolean enablePathBuffer) {
        this.curatorLockFactory = curatorLockFactory;
        this.maxResidualTime = maxResidualTime;
        this.maxChildNum = maxChildNum;
        this.cleanAllCountPeriod = cleanAllCountPeriod;
        if (enablePathBuffer) {
            zkCleaner.useBufferPath();
        }
    }

    @Override
    public void run() {
        logger.info("cleanup start");
        try {
            CuratorFramework curatorClient = curatorLockFactory.getCuratorClient();
            String rootPath = CuratorLockTools.getRootPath();

            long count = 0;
            if (cleanCount == cleanAllCountPeriod) {
                count = zkCleaner.clearResidualNode(curatorClient, rootPath, maxResidualTime);
                cleanCount = 0;
            }
            else {
                count = zkCleaner.cleanPathSetNode(curatorClient, maxChildNum, maxResidualTime);
                cleanCount++;
            }
            logger.info("cleanup end, delete node: {}", count);
        }
        catch (Exception e) {
            logger.error("execute CuratorCleanJob failed", e);
        }
    }
}
