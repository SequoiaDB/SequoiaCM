package com.sequoiacm.fulltext.server.lock;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;

@Component
public class LockManager {
    private static final Logger logger = LoggerFactory.getLogger(LockManager.class);

    private CuratorLockFactory innerFactory;
    private LockConfig lockConfig;

    @Autowired
    public LockManager(LockConfig conf) {
        this.lockConfig = conf;
    }

    private boolean initLockFactory() throws FullTextException {
        try {
            synchronized (LockManager.class) {
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
            throw new FullTextException(ScmError.LOCK_ERROR, "failed to init lock manager", e);
        }
        return true;
    }

    private void closeLockFactory() {
        if (innerFactory != null) {
            innerFactory.close();
            innerFactory = null;
        }
    }

    public ScmLock acquiresReadLock(LockPath lockPath) throws FullTextException {
        try {
            checkAndInit();
            ScmLock readLock = innerFactory.createReadWriteLock(lockPath.getPath()).readLock();
            readLock.lock();
            return readLock;
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.LOCK_ERROR,
                    "failed to acquires readlock:locakPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    public ScmLock acquiresWriteLock(LockPath lockPath) throws FullTextException {
        try {
            checkAndInit();
            ScmLock writeLock = innerFactory.createReadWriteLock(lockPath.getPath()).writeLock();
            writeLock.lock();
            return writeLock;
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.LOCK_ERROR,
                    "failed to acquires writelock:locakPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    public ScmLock acquiresLock(LockPath lockPath, long timeout) throws FullTextException {
        try {
            checkAndInit();
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            boolean isLockSuccess = lock.lock(timeout, TimeUnit.MILLISECONDS);
            if (isLockSuccess) {
                return lock;
            }
            return null;
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.LOCK_ERROR,
                    "failed to acquires lock:locakPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    public ScmLock acquiresLock(LockPath lockPath) throws FullTextException {
        try {
            checkAndInit();
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            lock.lock();
            return lock;
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.LOCK_ERROR,
                    "failed to acquires lock:locakPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    // return the lock if success, else return null
    public ScmLock tryAcquiresLock(LockPath lockPath) throws FullTextException {
        try {
            checkAndInit();
            ScmLock lock = innerFactory.createLock(lockPath.getPath());
            if (lock.tryLock()) {
                return lock;
            }
            return null;
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.LOCK_ERROR,
                    "try acquires lock failed:locakPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    private void checkAndInit() throws FullTextException {
        if (innerFactory == null) {
            boolean initSuccess = initLockFactory();
            if (!initSuccess) {
                throw new FullTextException(ScmError.LOCK_ERROR, "failed to init lock manager");
            }
        }
    }
}
