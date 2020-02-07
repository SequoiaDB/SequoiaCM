package com.sequoiacm.config.framework.lock;

import java.util.Arrays;

import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;

public class ScmLockManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmLockManager.class);

    private static ScmLockManager instance = new ScmLockManager();

    private static LockFactory innerFactory;
    private LockConfig lockConfig;

    public static ScmLockManager getInstance() {
        return instance;
    }

    private ScmLockManager() {
    }

    public void init(LockConfig lockConfig) throws ScmLockException {
        this.lockConfig = lockConfig;
        initLockFactory();
    }

    private boolean initLockFactory() throws ScmLockException {
        try {
            synchronized (ScmLockManager.class) {
                if (innerFactory == null) {
                    innerFactory = new CuratorLockFactory(lockConfig.getUrls(),
                            lockConfig.getClientNum());
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
            checkAndInit();
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
            checkAndInit();
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
            checkAndInit();
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
            checkAndInit();
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

    private void checkAndInit() throws ScmLockException {
        if (innerFactory == null) {
            boolean initSuccess = initLockFactory();
            if (!initSuccess) {
                throw new ScmLockException("failed to init lock manager");
            }
        }
    }
}
