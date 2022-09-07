package com.sequoiacm.infrastructure.lock.curator;

import com.sequoiacm.infrastructure.common.ZkAcl;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmReadWriteLock;
import com.sequoiacm.infrastructure.lock.ZKCompaticify;

public class CuratorLockFactory implements LockFactory {
    private static final Logger logger = LoggerFactory.getLogger(CuratorLockFromReadWrite.class);
    private CuratorFramework client = null;
    private boolean enableContainer;
    private ScmTimer t;

    private String zkUrl;
    private ZkAcl acl;

    public CuratorLockFactory(String zkUrl) throws Exception {
        this(zkUrl, new ZkAcl());
    }

    public CuratorLockFactory(String zkUrl, ZkAcl acl) throws Exception {
        // SEQUOIACM-485:enableContainer is false, zk server version below 3.5
        enableContainer = ZKCompaticify.enableContainer(zkUrl);
        this.zkUrl = zkUrl;
        this.acl = acl;
        try {
            logger.info("start init zookeeper client: zkUrl={}, enableContainer={}", zkUrl,
                    enableContainer);
            this.client = CuratorLockTools.createClient(zkUrl, enableContainer, acl);
        }
        catch (Exception e) {
            logger.error(
                    "Fail to init curator client:zkConnStr={}, enableContainer={}",
                    zkUrl, enableContainer);
            close();
            throw e;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }));
    }

    @Override
    public ScmLock createLock(String[] lockPath) {
        String path = CuratorLockTools.getLockPath(lockPath);
        return new CuratorLock(getCuratorClient(), path);
    }

    @Override
    public ScmReadWriteLock createReadWriteLock(String[] lockPath) {
        String path = CuratorLockTools.getLockPath(lockPath);
        return new CuratorReadWriteLock(getCuratorClient(), path);
    }

    public CuratorFramework getCuratorClient() {
        return client;
    }

    @Override
    public void close() {
        if (t != null) {
            t.cancel();
        }
        closeClient(client);
        closeZkCleaner();
    }

    private void closeZkCleaner() {
        if (t != null) {
            t.cancel();
        }
        if (CuratorZKCleaner.isInitialized()) {
            CuratorZKCleaner.getInstance().close();
        }
    }

    private void closeClient(CuratorFramework client) {
        try {
            if (null != client) {
                client.close();
            }
        }
        catch (Exception e) {
            logger.warn("Fail to close curator client!", e);
        }
    }

    @Override
    public void startCleanJob(long period, long maxResidualTime, int coreCleanThreads,
            int maxCleanThreads,
            int cleanQueueSize, int maxBuffer) {
        CuratorZKCleaner.init(getCuratorClient(), coreCleanThreads, maxCleanThreads,
                cleanQueueSize);
        if (!enableContainer) {
            if (null != t) {
                t.cancel();
                t = null;
            }

            t = ScmTimerFactory.createScmTimer();
            CuratorCleanJob task = new CuratorCleanJob(this, maxResidualTime, maxBuffer);
            t.schedule(task, 0, period);
            logger.info("start clean job cleanJobResidualTime={}, cleanJobPeriod={}",
                    maxResidualTime, period);
        }
        else {
            // enableContainer is true, clean only once
            logger.info("clean all old zookeeper node");
            CuratorCleanJob task = new CuratorCleanJob(this, 0, maxBuffer);
            t.schedule(task, 0);
        }
    }

    public CuratorFramework createCleanJobClient(int maxBuffer) throws Exception {
        System.setProperty("jute.maxbuffer", String.valueOf(maxBuffer));
        return CuratorLockTools.createClient(zkUrl, enableContainer, acl);
    }
}
