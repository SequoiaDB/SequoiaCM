package com.sequoiacm.contentserver.lock;

import java.util.Arrays;

public class ScmLockPath extends com.sequoiacm.infrastructure.lock.ScmLockPath {
    private String[] lockPath;

    ScmLockPath(String[] lockPath) {
        super(lockPath);
        this.lockPath = lockPath;
    }

    public String[] getPath() {
        return lockPath;
    }

    @Override
    public String toString() {
        return Arrays.toString(lockPath);
    }
}
