package com.sequoiacm.s3import.factory;

import com.amazonaws.services.s3.AmazonS3Client;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmFileResource;
import com.sequoiacm.infrastructure.tool.fileoperation.ScmResourceFactory;
import com.sequoiacm.s3import.common.CommonDefine;
import com.sequoiacm.s3import.common.ListContext;
import com.sequoiacm.s3import.common.S3Utils;
import com.sequoiacm.s3import.config.ImportToolProps;
import com.sequoiacm.s3import.config.S3ClientManager;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.module.S3ImportBatch;
import com.sequoiacm.s3import.module.*;
import com.sequoiacm.s3import.progress.MigrateProgress;
import com.sequoiacm.s3import.task.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class S3ImportBatchFactory {

    private static volatile S3ImportBatchFactory instance = null;

    private int batchSize = ImportToolProps.getInstance().getBatchSize();

    private S3ImportBatchFactory() throws ScmToolsException {

    }

    public static S3ImportBatchFactory getInstance() throws ScmToolsException {
        if (instance == null) {
            synchronized (S3ImportBatchFactory.class) {
                if (instance == null) {
                    instance = new S3ImportBatchFactory();
                }
            }
        }

        return instance;
    }

    public S3ImportBatch getNextBatch(S3Bucket s3Bucket, boolean isNeedOverWrite)
            throws ScmToolsException {
        // 获取 keyMarker，列取对象
        MigrateProgress progress = (MigrateProgress) s3Bucket.getProgress();
        String nextKeyMarker = progress.getNextKeyMarker();
        S3ClientManager clientManager = S3ClientManager.getInstance();
        AmazonS3Client srcS3Client = clientManager.getSrcS3Client();

        ListContext listContext = new ListContext();
        List<S3ImportObject> objectList = S3Utils.listS3Object(srcS3Client, s3Bucket.getName(),
                s3Bucket.isEnableVersionControl(), nextKeyMarker, batchSize, listContext);
        progress.setNextKeyMarker(listContext.getNextKeyMarker());

        List<S3ImportTask> taskList = new ArrayList<>();
        for (S3ImportObject s3ImportObject : objectList) {
            taskList.add(isNeedOverWrite ? new OverWriteTask(s3ImportObject, s3Bucket.getDestName())
                    : new MigrateTask(s3ImportObject, s3Bucket.getDestName()));
        }

        return taskList.size() == 0 ? null : new S3ImportBatch(s3Bucket, taskList);
    }

    public S3ImportBatch getNextBatchByErrorKeyList(S3Bucket s3Bucket) {
        Queue<String> errorKeyList = s3Bucket.getErrorKeyList();
        if (errorKeyList.size() == 0) {
            return null;
        }

        S3ImportBatch batch = new S3ImportBatch(s3Bucket);
        int count = 0;
        while (count++ < batchSize && errorKeyList.size() > 0) {
            String key = errorKeyList.poll();
            S3ImportObject s3ImportObject = new S3ImportObject(s3Bucket.getName(), key,
                    s3Bucket.isEnableVersionControl());
            batch.addTask(new OverWriteTask(s3ImportObject, s3Bucket.getDestName()));
        }
        return batch;
    }

    public S3ImportBatch getNextBatchByCmpResult(S3Bucket s3Bucket) throws ScmToolsException {
        ScmFileResource fileResource = s3Bucket.getResultFileResource();
        if (fileResource == null) {
            File compareResultFile = new File(s3Bucket.getCompareResultFilePath());
            fileResource = ScmResourceFactory.getInstance()
                    .createFileResource(compareResultFile);
            s3Bucket.setResultFileResource(fileResource);
        }
        S3ImportBatch batch = new S3ImportBatch(s3Bucket);
        int count = 0;
        while (count++ < batchSize) {
            String recordStr = fileResource.readLine();
            if (recordStr == null) {
                break;
            }
            CompareResult compareResult = new CompareResult(recordStr);
            batch.addTask(generateSyncTask(s3Bucket, compareResult));
        }
        return batch.getTaskList().size() != 0 ? batch : null;
    }

    private S3ImportTask generateSyncTask(S3Bucket s3Bucket, CompareResult compareResult)
            throws ScmToolsException {
        S3ImportTask task;
        String diffType = compareResult.getDiffType();
        S3ImportObject importObject = new S3ImportObject(s3Bucket.getName(), compareResult.getKey(),
                s3Bucket.isEnableVersionControl());
        switch (diffType) {
            case CommonDefine.DiffType.NEW:
                task = new SyncTask(importObject, s3Bucket.getDestName(),
                        CommonDefine.SyncType.ADD);
                break;
            case CommonDefine.DiffType.DIFF_VERSION:
            case CommonDefine.DiffType.DIFF_METADATA:
            case CommonDefine.DiffType.DIFF_CONTENT:
                task = new SyncTask(importObject, s3Bucket.getDestName(),
                        CommonDefine.SyncType.UPDATE);
                break;
            case CommonDefine.DiffType.DELETED:
                task = new SyncTask(importObject, s3Bucket.getDestName(),
                        CommonDefine.SyncType.DELETE);
                break;
            default:
                throw new ScmToolsException("Unrecognized diff type, type=" + diffType,
                        S3ImportExitCode.INVALID_ARG);
        }
        return task;
    }

}
