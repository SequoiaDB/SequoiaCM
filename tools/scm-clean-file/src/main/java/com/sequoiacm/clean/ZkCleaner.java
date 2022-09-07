package com.sequoiacm.clean;

import com.sequoiacm.infrastructure.lock.ScmLockConfig;
import com.sequoiacm.infrastructure.lock.curator.CuratorCleanJob;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;
import com.sequoiacm.infrastructure.lock.curator.CuratorZKCleaner;

public class ZkCleaner {
    private final String zkUrls;
    private final int maxBuffer;
    private long maxResidualTime;
    private int maxChildNum;

    public ZkCleaner(String zkUrls, int maxBuffer, long maxResidualTime, int maxChildNum) {
        this.zkUrls = zkUrls;
        this.maxBuffer = maxBuffer;
        this.maxResidualTime = maxResidualTime;
        this.maxChildNum = maxChildNum;
        System.setProperty("jute.maxbuffer", (maxBuffer * 1024 * 1024) + "");
    }

    public void cleanZk() throws Exception {
        CuratorLockFactory factory = new CuratorLockFactory(zkUrls);
        if (!CuratorZKCleaner.isInitialized()) {
            ScmLockConfig lockConfig = new ScmLockConfig();
            CuratorZKCleaner.init(factory.getCuratorClient(), lockConfig.getCoreCleanThreads(),
                    lockConfig.getMaxCleanThreads(),
                    lockConfig.getCleanQueueSize());
        }
        CuratorCleanJob job = new CuratorCleanJob(factory, maxResidualTime,
                maxBuffer * 1024 * 1024);
        job.run();
    }
}
