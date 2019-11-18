package com.sequoiacm.schedule.core;

import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.curator.CuratorLockFactory;

public class ScheduleLockFactory {
    private static ScheduleLockFactory instance = new ScheduleLockFactory();

    private ScheduleLockFactory() {
    }

    private LockFactory lockFactory = null;

    public static ScheduleLockFactory getInstance() {
        return instance;
    }

    public void init(String zkConn, int clientCacheNum) throws Exception {
        lockFactory = new CuratorLockFactory(zkConn, clientCacheNum);
    }

    public ScmLock createLock(String[] lockPath) {
        return lockFactory.createLock(lockPath);
    }
}
