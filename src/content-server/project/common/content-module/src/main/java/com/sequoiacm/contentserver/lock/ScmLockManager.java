package com.sequoiacm.contentserver.lock;

import java.util.Arrays;

import com.sequoiacm.infrastructure.lock.ScmLockConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.exception.ScmLockException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class ScmLockManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmLockManager.class);

    private static ScmLockManager instance = new ScmLockManager();

    private com.sequoiacm.infrastructure.lock.ScmLockManager innerLockManager;

    public static ScmLockManager getInstance() throws ScmServerException {
        return instance;
    }

    private ScmLockManager() {
    }

    public com.sequoiacm.infrastructure.lock.ScmLockManager getInnerLockManager() {
        return innerLockManager;
    }

    public boolean init(ScmLockConfig lockConfig) throws ScmServerException {
        try {
            this.innerLockManager = new com.sequoiacm.infrastructure.lock.ScmLockManager(
                    lockConfig);
            return this.innerLockManager.initLockFactory();
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

    public ScmLock acquiresReadLock(ScmLockPath lockPath) throws ScmServerException {
        try {
            return innerLockManager.acquiresReadLock(lockPath);
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires readlock:lockPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    public ScmLock acquiresWriteLock(ScmLockPath lockPath) throws ScmServerException {
        try {
            return innerLockManager.acquiresWriteLock(lockPath);
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires writelock:lockPath=" + Arrays.toString(lockPath.getPath()),
                    e);
        }
    }

    // return null if timeout!
    public ScmLock acquiresLock(ScmLockPath lockPath, long timeoutInMs) throws ScmServerException {
        try {

            return innerLockManager.acquiresLock(lockPath, timeoutInMs);
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires lock:lockPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    public ScmLock acquiresLock(ScmLockPath lockPath) throws ScmServerException {
        try {
            return innerLockManager.acquiresLock(lockPath);
        }
        catch (Exception e) {
            throw new ScmLockException(
                    "failed to acquires lock:lockPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

    // return the lock if success, else return null
    public ScmLock tryAcquiresLock(ScmLockPath lockPath) throws ScmServerException {
        try {
            return innerLockManager.tryAcquiresLock(lockPath);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.LOCK_ERROR,
                    "try acquires lock failed:lockPath=" + Arrays.toString(lockPath.getPath()), e);
        }
    }

}
