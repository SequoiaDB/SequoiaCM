package com.sequoiacm.daemon.lock;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;

public class ScmFileResourceFactory {
    private static volatile ScmFileResourceFactory instance;

    public static ScmFileResourceFactory getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ScmFileResourceFactory.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ScmFileResourceFactory();
            return instance;
        }
    }

    private ScmFileResourceFactory() {
    }

    public ScmFileResource createFileResource(File file) throws ScmToolsException {
        return new ScmFileResource(file);
    }

    public ScmFileResource createFileResource(File file, String backUpPath)
            throws ScmToolsException {
        return new ScmFileResource(file, backUpPath);
    }
}
