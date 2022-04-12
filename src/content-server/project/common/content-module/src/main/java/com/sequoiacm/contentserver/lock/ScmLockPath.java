package com.sequoiacm.contentserver.lock;

import java.util.Arrays;

public class ScmLockPath {
    private String[] lockPath;

    ScmLockPath(String[] lockPath) {
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
