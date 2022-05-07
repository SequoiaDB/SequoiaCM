package com.sequoiacm.s3import.task;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.module.S3ImportBatch;
import com.sequoiacm.s3import.module.S3ImportObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class S3ImportTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(S3ImportTask.class);

    protected S3ImportBatch batch;
    protected S3ImportObject importObject;
    protected String destBucket;

    public S3ImportTask(S3ImportObject importObject, String destBucket) {
        this.importObject = importObject;
        this.destBucket = destBucket;
    }

    public void setBatch(S3ImportBatch batch) {
        this.batch = batch;
    }

    public void run() {
        try {
            doTask();
            batch.success();
        }
        catch (Exception e) {
            logger.error("Task execution failed, bucket={}, dest_bucket={}, key={}",
                    importObject.getBucket(), destBucket, importObject.getKey(), e);
            batch.failed();
            batch.addErrorKey(importObject.getKey());
        }
    }

    public abstract void doTask() throws ScmToolsException;
}
