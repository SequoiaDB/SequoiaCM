package com.sequoiacm.infrastructure.lock.curator;

import java.util.concurrent.TimeUnit;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
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
    @SlowLog(operation = "releaseLock")
    public void unlock() {
        boolean isAcquiredInThisProcess = false;
        try {
            if (this.lock.isAcquiredInThisProcess()) {
                isAcquiredInThisProcess = true;
                this.lock.release();
            }
        }
        catch (Exception e) {
            logger.warn("Fail to release curator mutex lock:lockPath={}", lockPath, e);
        }
        finally {
            if (isAcquiredInThisProcess) {
                try {
                    CuratorZKCleaner.getInstance().putPath(lockPath);
                }
                catch (Exception e) {
                    logger.warn("Failed to put path into CuratorZKCleaner, lockPath={}", lockPath,
                            e);
                }
            }
        }
    }

    @Override
    @SlowLog(operation = "acquireLock", extras = @SlowLogExtra(name = "lockPath", data = "lockPath"))
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
    @SlowLog(operation = "acquireLock", extras = @SlowLogExtra(name = "lockPath", data = "lockPath"))
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
    @SlowLog(operation = "tryLock", extras = @SlowLogExtra(name = "tryLockPath", data = "lockPath"))
    public boolean tryLock() throws Exception {
        try {
            return lock.acquire(CuratorLockProperty.TRYLOCK_WAITTIME, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
            logger.error("Try to acquire curator mutex lock failed within the waitTime:lockPath={}",
                    lockPath);
            throw e;
        }
    }
}
