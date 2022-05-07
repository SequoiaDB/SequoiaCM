package com.sequoiacm.s3import.factory;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.dao.S3ObjectImportDao;
import com.sequoiacm.s3import.dao.S3ObjectImportDaoImpl;
import com.sequoiacm.s3import.module.S3ImportObject;

public class S3ObjectImportDaoFactory {

    private static volatile S3ObjectImportDaoFactory instance = null;

    private void S3ObjectImportDaoFactory() {

    }

    public static S3ObjectImportDaoFactory getInstance() {
        if (instance == null) {
            synchronized (S3ObjectImportDaoFactory.class) {
                if (instance == null) {
                    instance = new S3ObjectImportDaoFactory();
                }
            }
        }

        return instance;
    }

    public S3ObjectImportDao createObjectImportDao(S3ImportObject s3ImportObject, String destBucket)
            throws ScmToolsException {
        return new S3ObjectImportDaoImpl(s3ImportObject, destBucket);
    }

}
