package com.sequoiacm.clean;

import com.sequoiacm.infrastructure.lock.curator.CuratorCleanJob;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;

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
        CuratorCleanJob job = new CuratorCleanJob(factory, maxResidualTime, maxChildNum, 0, false);
        job.run();
    }
}
