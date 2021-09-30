package com.sequoiacm.daemon.lock;

import java.io.File;

public class ScmFileLockFactory {
    private static volatile ScmFileLockFactory instance;

    public static ScmFileLockFactory getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmFileLockFactory.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmFileLockFactory();
            return instance;
        }
    }

    private ScmFileLockFactory() {
    }

    public ScmFileLock createFileLock(File file) {
        return new ScmFileLock(file);
    }
}
