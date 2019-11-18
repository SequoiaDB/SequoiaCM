package com.sequoiacm.infrastructure.lock.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;

import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmReadWriteLock;

class CuratorReadWriteLock implements ScmReadWriteLock {
    private InterProcessReadWriteLock readWriteLock;

    public CuratorReadWriteLock(CuratorFramework client, String lockPath) {
        readWriteLock = new InterProcessReadWriteLock(client, lockPath);
    }

    @Override
    public ScmLock readLock() {
        return new CuratorLockFromReadWrite(readWriteLock.readLock());
    }

    @Override
    public ScmLock writeLock() {
        return new CuratorLockFromReadWrite(readWriteLock.writeLock());
    }
}
