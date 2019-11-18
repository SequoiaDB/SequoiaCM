package com.sequoiacm.infrastructure.lock.curator;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.lock.ScmLock;

class CuratorLock implements ScmLock {
    private static final Logger logger = LoggerFactory.getLogger(CuratorLock.class);
    private InterProcessMutex lock;
    private String lockPath;

    public CuratorLock(CuratorFramework client, String lockPath) {
        this.lock = new InterProcessMutex(client, lockPath);
        this.lockPath = lockPath;
    }

    @Override
    public void unlock() {

        try {
            if (this.lock.isAcquiredInThisProcess()) {
                this.lock.release();
            }
        }
        catch (Exception e) {
            logger.warn("Fail to relese curator mutex lock:lockPath={}", lockPath, e);
        }
    }

    @Override
    public void lock() throws Exception {
        try {
            this.lock.acquire();
        }
        catch (Exception e) {
            logger.error("Fail to acquire curator mutex lock:lockPath={}", lockPath);
            throw e;
        }
    }

    @Override
    public boolean lock(long waitTime, TimeUnit unit) throws Exception {
        try {
            return lock.acquire(waitTime, unit);
        }
        catch (Exception e) {
            logger.error("Fail to acquire curator mutex lock within the waitTime:lockPath={}",
                    lockPath);
            throw e;
        }
    }

    @Override
    public boolean tryLock() throws Exception {
        try {
            return lock.acquire(CuratorLockProperty.TRYLOCK_WAITTIME, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
            logger.error("Try to acquire curator mutex lock faild within the waitTime:lockPath={}",
                    lockPath);
            throw e;
        }
    }
}
