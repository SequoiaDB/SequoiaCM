package com.sequoiacm.infrastructure.lock.fake;

import com.sequoiacm.infrastructure.lock.LockFactory;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmReadWriteLock;

public class FakeLockFactory implements LockFactory {

    @Override
    public ScmLock createLock(String[] lockPath) {
        return new FakeLock();
    }

    @Override
    public ScmReadWriteLock createReadWriteLock(String[] lockPath) {
        return new FakeReadWriteLock();
    }

    @Override
    public void close() {
    }

    @Override
    public void startCleanJob(long period, long maxResidualTime, int maxChildNum, int cleanCount) {

    }
}
