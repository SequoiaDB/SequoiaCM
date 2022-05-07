package com.sequoiacm.s3import.task;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonDefine;
import com.sequoiacm.s3import.module.S3ImportObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncTask extends S3ImportTask {

    private static final Logger logger = LoggerFactory.getLogger(SyncTask.class);

    private S3ImportTask innerTask;
    private String syncType;

    public SyncTask(S3ImportObject s3ImportObject, String destBucket, String syncType) {
        super(s3ImportObject, destBucket);
        this.syncType = syncType;
        this.innerTask = syncType.equals(CommonDefine.SyncType.DELETE)
                ? new DeleteTask(s3ImportObject, destBucket)
                : new OverWriteTask(s3ImportObject, destBucket);
    }

    @Override
    public void run() {
        try {
            doTask();
            batch.success(syncType);
        }
        catch (ScmToolsException e) {
            logger.error(
                    "Sync task execution failed, sync_type={}, bucket={}, dest_bucket={}, key={}",
                    syncType, importObject.getBucket(), destBucket, importObject.getKey(), e);
            batch.failed();
            batch.addErrorSyncKey(importObject.getKey(), syncType);
        }
    }

    @Override
    public void doTask() throws ScmToolsException {
        innerTask.doTask();
    }
}
