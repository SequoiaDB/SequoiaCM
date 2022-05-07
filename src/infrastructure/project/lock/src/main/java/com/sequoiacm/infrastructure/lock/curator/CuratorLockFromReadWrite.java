package com.sequoiacm.infrastructure.lock.curator;

import java.util.concurrent.TimeUnit;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.lock.ScmLock;

class CuratorLockFromReadWrite implements ScmLock {
    private static final Logger logger = LoggerFactory.getLogger(CuratorLockFromReadWrite.class);
    private InterProcessMutex lock;
    private String lockPath;

    public CuratorLockFromReadWrite(InterProcessMutex lock, String lockPath) {
        this.lock = lock;
        this.lockPath = lockPath;
    }

    @Override
    @SlowLog(operation = "releaseLock")
    public void unlock() {
        try {
            if (this.lock.isAcquiredInThisProcess()) {
                this.lock.release();
            }
        }
        catch (Exception e) {
            logger.warn("Fail to relese curator read or write lock!", e);
        }
    }

    @Override
    @SlowLog(operation = "acquireLock", extras = @SlowLogExtra(name = "lockPath", data = "lockPath"))
    public void lock() throws Exception {
        try {
            this.lock.acquire();
        }
        catch (Exception e) {
            logger.error("Fail to acquire curator read or write lock!");
            throw e;
        }
    }

    @Override
    @SlowLog(operation = "acquireLock", extras = @SlowLogExtra(name = "lockPath", data = "lockPath"))
    public boolean lock(long waitTime, TimeUnit unit) throws Exception {
        try {
            return this.lock.acquire(waitTime, unit);
        }
        catch (Exception e) {
            logger.error("Fail to acquire curator read or write lock within the waitTime!");
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
            logger.error("Try to acquire  curator read or write lock faild within the waitTime!");
            throw e;
        }
    }
}
