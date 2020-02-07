package com.sequoiacm.contentserver.lock;

import java.util.Arrays;

import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.exception.ScmLockException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;

public class ScmLockManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmLockManager.class);

    private static ScmLockManager instance = new ScmLockManager();

    private static LockFactory innerFactory;

    public static ScmLockManager getInstance() throws ScmServerException {
        return instance;
    }

    private ScmLockManager() {
    }

    public boolean init() throws ScmServerException {
        try {
            synchronized (ScmLockManager.class) {
                if (innerFactory == null) {
                    innerFactory = new CuratorLockFactory(PropertiesUtils.getZKConnUrl(),
                            PropertiesUtils.getZkClientNum());
                    innerFactory.startCleanJob(PropertiesUtils.getZKCleanJobPeriod(),
                            PropertiesUtils.getZKCleanJobResidual(),
                            PropertiesUtils.getClenaJobChildThreshold(),
                            PropertiesUtils.getClenaJobCountThreshold());
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

    public ScmLock acquiresReadLock(ScmLockPath lockPath) throws ScmServerException {
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

    public ScmLock acquiresWriteLock(ScmLockPath lockPath) throws ScmServerException {
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

    public ScmLock acquiresLock(ScmLockPath lockPath) throws ScmServerException {
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
    public ScmLock tryAcquiresLock(ScmLockPath lockPath) throws ScmServerException {
        try {
            checkAndInit();
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            if (lock.tryLock()) {
                return lock;
            }
            return null;
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.LOCK_ERROR,
                    "try acquires lock failed:locakPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    private void checkAndInit() throws ScmServerException {
        if (innerFactory == null) {
            boolean initSuccess = init();
            if (!initSuccess) {
                throw new ScmLockException("failed to init lock manager");
            }
        }
    }
}
