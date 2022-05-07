package com.sequoiacm.s3import.task;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.dao.S3ObjectImportDao;
import com.sequoiacm.s3import.factory.S3ObjectImportDaoFactory;
import com.sequoiacm.s3import.module.S3ImportObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateTask extends S3ImportTask {

    private static final Logger logger = LoggerFactory.getLogger(MigrateTask.class);

    public MigrateTask(S3ImportObject s3ImportObject, String destBucket) {
        super(s3ImportObject, destBucket);
    }

    @Override
    public void doTask() throws ScmToolsException {
        S3ObjectImportDaoFactory factory = S3ObjectImportDaoFactory.getInstance();
        S3ObjectImportDao objectImportDao = factory.createObjectImportDao(importObject, destBucket);
        try {
            objectImportDao.create();
        }
        catch (Exception e) {
            try {
                objectImportDao.delete();
            }
            catch (Exception ex) {
                batch.setHasAbortedTask(true);
                logger.error("Failed to clear the residual object data, dest_bucket={}, key={}",
                        destBucket, importObject, ex);
            }
            logger.error("Migration of object failed, bucket={}, dest_bucket={}, key={}",
                    importObject.getBucket(), destBucket, importObject.getKey());
            throw e;
        }
    }
}
