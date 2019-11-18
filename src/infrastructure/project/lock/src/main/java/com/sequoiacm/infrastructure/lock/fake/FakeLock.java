package com.sequoiacm.infrastructure.lock.fake;

import java.util.concurrent.TimeUnit;

import com.sequoiacm.infrastructure.lock.ScmLock;

public class FakeLock implements ScmLock {

    @Override
    public void unlock() {
    }

    @Override
    public void lock() throws Exception {
    }

    @Override
    public boolean lock(long waitTime, TimeUnit unit) throws Exception {
        return true;
    }

    @Override
    public boolean tryLock() throws Exception {
        return true;
    }

}
