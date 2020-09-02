package com.sequoiacm.infrastructure.lock;

public interface LockFactory {
    public abstract ScmLock createLock(String[] lockPath);

    public abstract ScmReadWriteLock createReadWriteLock(String[] lockPath);

    public abstract void close();

    public abstract void startCleanJob(long period, long maxResidualTime, int maxChildNum,
            int cleanCount);

}
