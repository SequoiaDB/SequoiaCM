package com.sequoiacm.s3import.task;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.dao.S3ObjectImportDao;
import com.sequoiacm.s3import.factory.S3ObjectImportDaoFactory;
import com.sequoiacm.s3import.module.S3ImportObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteTask extends S3ImportTask {

    private static final Logger logger = LoggerFactory.getLogger(DeleteTask.class);

    public DeleteTask(S3ImportObject s3ImportObject, String destBucket) {
        super(s3ImportObject, destBucket);
    }

    @Override
    public void doTask() throws ScmToolsException {
        S3ObjectImportDaoFactory factory = S3ObjectImportDaoFactory.getInstance();
        S3ObjectImportDao objectImportDao = factory.createObjectImportDao(importObject, destBucket);
        try {
            objectImportDao.delete();
        }
        catch (Exception e) {
            logger.error("Failed to delete object, dest_bucket={}, key={}", destBucket,
                    importObject);
            throw e;
        }
    }
}
