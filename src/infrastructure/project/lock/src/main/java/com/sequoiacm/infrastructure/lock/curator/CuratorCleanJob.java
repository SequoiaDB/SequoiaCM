package com.sequoiacm.infrastructure.lock.curator;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

public class CuratorCleanJob extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(CuratorCleanJob.class);
    private String zkConnStr;
    private boolean enableContainer;
    private long maxResidualTime;
    private int cleanAllCountPeriod;
    private int maxChildNum;
    private CuratorZKCleaner zkCleaner = CuratorZKCleaner.getInstance();
    private int cleanCount = 0;

    public CuratorCleanJob(String zkConnStr, boolean enableContainer, long maxResidualTime,
            int maxChildNum, int cleanAllCountPeriod, boolean enablePathBuffer) {
        this.zkConnStr = zkConnStr;
        this.enableContainer = enableContainer;
        this.maxResidualTime = maxResidualTime;
        this.maxChildNum = maxChildNum;
        this.cleanAllCountPeriod = cleanAllCountPeriod;
        if (enablePathBuffer) {
            zkCleaner.useBufferPath();
        }
    }

    @Override
    public void run() {
        logger.debug("cleanup start");
        CuratorFramework curatorClient = null;
        try {
            curatorClient = CuratorLockTools.createClient(zkConnStr, enableContainer);
            String rootPath = CuratorLockTools.getRootPath();
            if (cleanCount == cleanAllCountPeriod) {
                zkCleaner.clearResidualNode(curatorClient, rootPath, maxResidualTime);
                cleanCount = 0;
            }
            else {
                zkCleaner.cleanPathSetNode(curatorClient, maxChildNum, maxResidualTime);
                cleanCount++;
            }
            logger.debug("cleanup end");
        }
        catch (Exception e) {
            logger.error("Execute CuratorCleanJob failed", e);
        }
        finally {
            CuratorZKBase.closeClient(curatorClient);
            curatorClient = null;
        }
    }
}
