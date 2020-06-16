package com.sequoiacm.mq.server.lock;

import java.util.Arrays;

public class LockPath {
    private String[] lockPath;

    LockPath(String[] lockPath) {
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
