package com.sequoiacm.infrastructure.lock.fake;

import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmReadWriteLock;

public class FakeReadWriteLock implements ScmReadWriteLock {

    @Override
    public ScmLock readLock() {
        return new FakeLock();
    }

    @Override
    public ScmLock writeLock() {
        return new FakeLock();
    }

}
