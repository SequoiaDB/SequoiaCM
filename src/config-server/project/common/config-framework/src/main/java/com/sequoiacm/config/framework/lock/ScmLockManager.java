package com.sequoiacm.config.framework.lock;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;

public class ScmLockManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmLockManager.class);

    private static ScmLockManager instance = new ScmLockManager();

    private static LockFactory innerFactory;

    public static ScmLockManager getInstance() {
        return instance;
    }

    private ScmLockManager() {
    }

    public void init(String url, int zkClientNum, long cleanJobPeriod, long maxResidualTime)
            throws ScmLockException {
        try {
            innerFactory = new CuratorLockFactory(url, zkClientNum);

            logger.info("starting lock clean job:period={},maxResidualTime={}", cleanJobPeriod,
                    maxResidualTime);
            innerFactory.startCleanJob(cleanJobPeriod, maxResidualTime);
        }
        catch (Exception e) {
            throw new ScmLockException("failed to init lock manager", e);
        }
    }

    // public ScmReadWriteLock createReadWriteLock(ScmLockPath lockPath) throws
    // ScmLockException {
    // try {
    // return innerFactory.createReadWriteLock(lockPath.getPath());
    // }
    // catch (Exception e) {
    // throw new ScmLockException("failed to acquires readlock:locakPath="
    // + Arrays.toString(lockPath.getPath()), e);
    // }
    // }

    public ScmLock acquiresReadLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            ScmLock readLock = innerFactory.createReadWriteLock(lockPath.getPath()).readLock();
            readLock.lock();
            return readLock;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires readlock:locakPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    public ScmLock acquiresWriteLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            ScmLock writeLock = innerFactory.createReadWriteLock(lockPath.getPath()).writeLock();
            writeLock.lock();
            return writeLock;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires writelock:locakPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    public ScmLock acquiresLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            lock.lock();
            return lock;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires lock:locakPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    // return the lock if success, else return null
    public ScmLock tryAcquiresLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            if (lock.tryLock()) {
                return lock;
            }
            return null;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "try acquires lock failed:locakPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }
}
