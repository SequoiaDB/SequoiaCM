package com.sequoiacm.infrastructure.lock.curator;

import com.sequoiacm.infrastructure.lock.exception.ZkPacketLenOutOfRangeException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.client.ZKClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

public class CuratorCleanJob extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(CuratorCleanJob.class);
    private CuratorLockFactory curatorLockFactory;
    private long maxResidualTime;
    private int maxBuffer;
    private int currentBuffer = ZKClientConfig.CLIENT_MAX_PACKET_LENGTH_DEFAULT; // 4M

    public CuratorCleanJob(CuratorLockFactory curatorLockFactory, long maxResidualTime,
            int maxBuffer) {
        this.curatorLockFactory = curatorLockFactory;
        this.maxResidualTime = maxResidualTime;
        if (maxBuffer <= 0) {
            this.maxBuffer = getDefaultBufferSize();
            logger.info("use default maxBufferSize:{}", this.maxBuffer);
        }
        else {
            this.maxBuffer = maxBuffer;
        }

    }

    private int getDefaultBufferSize() {
        long maxHeapSize = Runtime.getRuntime().maxMemory();
        long maxBuffer = maxHeapSize / 5;
        if (maxBuffer > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) maxBuffer;
    }

    @Override
    public void run() {
        logger.info("zookeeper node cleanup start");
        try {
            CuratorFramework client = null;
            boolean shouldContinue = true;
            long count = 0;
            while (shouldContinue) {
                try {
                    client = curatorLockFactory.createCleanJobClient(currentBuffer);
                    String rootPath = CuratorLockTools.getRootPath();
                    count = CuratorZKCleaner.getInstance().clearResidualNode(client, rootPath,
                            maxResidualTime);
                    shouldContinue = false;
                }
                catch (ZkPacketLenOutOfRangeException e) {
                    if (currentBuffer < maxBuffer) {
                        int oldBuffer = currentBuffer;
                        currentBuffer = Math.min(currentBuffer * 2, maxBuffer);
                        shouldContinue = true;
                        logger.warn(
                                "zookeeper buffer size:{} maybe too low, increase to {} and try again",
                                oldBuffer, currentBuffer);
                    }
                    else {
                        shouldContinue = false;
                        throw e;
                    }
                }
                finally {
                    if (client != null) {
                        client.close();
                    }
                }
            }
            logger.info("zookeeper node cleanup end, delete node: {}", count);
        }
        catch (Exception e) {
            logger.error("execute CuratorCleanJob failed", e);
        }
    }
}
