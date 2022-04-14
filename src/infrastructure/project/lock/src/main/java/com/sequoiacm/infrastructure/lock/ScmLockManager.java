package com.sequoiacm.infrastructure.lock;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.infrastructure.lock.exception.ScmLockTimeoutException;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;

public class ScmLockManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmLockManager.class);

    private CuratorLockFactory innerFactory;
    private ScmLockConfig lockConfig;

    public ScmLockManager(ScmLockConfig conf) {
        this.lockConfig = conf;
    }

    private boolean initLockFactory() throws ScmLockException {
        try {
            synchronized (ScmLockManager.class) {
                if (innerFactory == null) {
                    innerFactory = new CuratorLockFactory(lockConfig.getUrls());
                    innerFactory.startCleanJob(lockConfig.getCleanJobPeriod(),
                            lockConfig.getCleanJobResidualTime(),
                            lockConfig.getClenaJobChildThreshold(),
                            lockConfig.getClenaJobCountThreshold());
                }
            }
        }
        catch (ConnectionLossException e) {
            closeLockFactory();
            logger.warn("zookeeper connection loss, failed to init lock manager", e);
            return false;
        }
        catch (Exception e) {
            closeLockFactory();
            throw new ScmLockException("failed to init lock manager", e);
        }
        return true;
    }

    private void closeLockFactory() {
        if (innerFactory != null) {
            innerFactory.close();
            innerFactory = null;
        }
    }

    public ScmLock acquiresReadLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            checkAndInit();
            ScmLock readLock = innerFactory.createReadWriteLock(lockPath.getPath()).readLock();
            readLock.lock();
            return readLock;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires readlock: lockPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    public ScmLock acquiresWriteLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            checkAndInit();
            ScmLock writeLock = innerFactory.createReadWriteLock(lockPath.getPath()).writeLock();
            writeLock.lock();
            return writeLock;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires writeLock: lockPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    public ScmLock acquiresReadLock(ScmLockPath lockPath, long timeoutInMs)
            throws ScmLockException, ScmLockTimeoutException {
        try {
            checkAndInit();
            ScmLock readLock = innerFactory.createReadWriteLock(lockPath.getPath()).readLock();
            if (readLock.lock(timeoutInMs, TimeUnit.MILLISECONDS)) {
                return readLock;
            }
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires read lock. lockPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }

        throw new ScmLockTimeoutException(
                "acquire read lock timeout. lockPath=" + Arrays.toString(lockPath.getPath()));
    }

    public ScmLock acquiresWriteLock(ScmLockPath lockPath, long timeoutInMs)
            throws ScmLockException, ScmLockTimeoutException {
        try {
            checkAndInit();
            ScmLock writeLock = innerFactory.createReadWriteLock(lockPath.getPath()).writeLock();
            if (writeLock.lock(timeoutInMs, TimeUnit.MILLISECONDS)) {
                return writeLock;
            }
        }
        catch (Exception e) {
            throw new ScmLockException("failed to acquires write lock. lockPath="
                    + Arrays.toString(lockPath.getPath()), e);
        }

        throw new ScmLockTimeoutException(
                "acquire write lock timeout. lockPath=" + Arrays.toString(lockPath.getPath()));
    }

    public ScmLock acquiresLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            checkAndInit();
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            lock.lock();
            return lock;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires lock:lockPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    // return the lock if success, else return null
    public ScmLock tryAcquiresLock(ScmLockPath lockPath) throws ScmLockException {
        try {
            checkAndInit();
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            if (lock.tryLock()) {
                return lock;
            }
            return null;
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "try acquires lock failed: lockPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    private void checkAndInit() throws ScmLockException {
        if (innerFactory == null) {
            boolean initSuccess = initLockFactory();
            if (!initSuccess) {
                throw new ScmLockException("failed to init lock manager");
            }
        }
    }
}
