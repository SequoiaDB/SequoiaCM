package com.sequoiacm.infrastructure.common;

import java.util.concurrent.locks.Lock;

public class ScmLockWrapper {

    private Lock lock;

    private boolean isReadLock;

    public ScmLockWrapper(Lock lock, boolean isReadLock) {
        this.lock = lock;
        this.isReadLock = isReadLock;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean isReadLock() {
        return isReadLock;
    }

    public boolean isWriteLock() {
        return !isReadLock;
    }

}
