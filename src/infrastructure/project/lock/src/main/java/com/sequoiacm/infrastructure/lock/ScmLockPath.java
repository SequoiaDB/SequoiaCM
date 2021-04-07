package com.sequoiacm.infrastructure.lock;

import java.util.Arrays;

public class ScmLockPath {
    private String[] lockPath;

    public ScmLockPath(String[] lockPath) {
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
