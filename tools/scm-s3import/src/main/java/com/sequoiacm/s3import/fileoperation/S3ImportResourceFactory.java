package com.sequoiacm.s3import.fileoperation;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonUtils;

import java.io.File;

public class S3ImportResourceFactory {

    private static volatile S3ImportResourceFactory instance;

    private S3ImportResourceFactory() {

    }

    public static S3ImportResourceFactory getInstance() {
        if (instance == null) {
            synchronized (S3ImportResourceFactory.class) {
                if (instance == null) {
                    instance = new S3ImportResourceFactory();
                }
            }
        }

        return instance;
    }

    public S3ImportFileResource createFileResource(File file)
            throws ScmToolsException {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            CommonUtils.createDir(file.getParent());
        }
        return new S3ImportFileResource(file);
    }
}
