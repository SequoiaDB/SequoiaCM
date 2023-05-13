package com.sequoiacm.infrastructure.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScmHashSlotLock {

    private final Map<Integer, ReadWriteLock> lockMap;
    private final int lockSize;

    public ScmHashSlotLock(int lockSize) {
        this.lockSize = lockSize;
        lockMap = new ConcurrentHashMap<Integer, ReadWriteLock>(lockSize);
    }

    public ScmLockWrapper getReadLock(String key) {
        checkNotNull(key);
        int position = getHash(key) % lockSize;
        ReadWriteLock readWriteLock = getLock(position);
        return new ScmLockWrapper(readWriteLock.readLock(), true);
    }

    public ScmLockWrapper getWriteLock(String key) {
        checkNotNull(key);
        int position = getHash(key) % lockSize;
        ReadWriteLock readWriteLock = getLock(position);
        return new ScmLockWrapper(readWriteLock.writeLock(), false);
    }

    private void checkNotNull(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key can not be null");
        }
    }

    private ReadWriteLock getLock(int position) {
        ReadWriteLock readWriteLock = lockMap.get(position);
        if (readWriteLock == null) {
            synchronized (lockMap) {
                readWriteLock = lockMap.get(position);
                if (readWriteLock == null) {
                    readWriteLock = new ReentrantReadWriteLock();
                    lockMap.put(position, readWriteLock);
                }
            }
        }
        return readWriteLock;
    }

    private int getHash(String key) {
        return Math.abs(key.hashCode());
    }
}
