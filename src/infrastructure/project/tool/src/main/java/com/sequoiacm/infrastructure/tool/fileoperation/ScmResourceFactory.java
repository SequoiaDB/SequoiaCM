package com.sequoiacm.infrastructure.tool.fileoperation;

import java.io.File;

import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmResourceFactory {

    private static volatile ScmResourceFactory instance;

    private ScmResourceFactory() {

    }

    public static ScmResourceFactory getInstance() {
        if (instance == null) {
            synchronized (ScmResourceFactory.class) {
                if (instance == null) {
                    instance = new ScmResourceFactory();
                }
            }
        }

        return instance;
    }

    public ScmFileResource createFileResource(File file)
            throws ScmToolsException {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            ScmCommon.createDir(file.getParent());
        }
        return new ScmFileResource(file);
    }
}
