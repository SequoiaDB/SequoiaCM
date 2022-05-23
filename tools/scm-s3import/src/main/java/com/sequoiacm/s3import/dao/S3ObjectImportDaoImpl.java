package com.sequoiacm.s3import.dao;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.client.ScmS3Client;
import com.sequoiacm.s3import.common.CommonDefine;
import com.sequoiacm.s3import.common.S3Utils;
import com.sequoiacm.s3import.config.S3ClientManager;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import com.sequoiacm.s3import.module.S3ImportObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class S3ObjectImportDaoImpl implements S3ObjectImportDao {

    private final static Logger logger = LoggerFactory.getLogger(S3ObjectImportDaoImpl.class);

    private AmazonS3Client srcClient;
    private AmazonS3Client destClient;
    private ScmS3Client destScmS3Client;
    private S3ImportObject importObject;
    private String destBucket;

    public S3ObjectImportDaoImpl(S3ImportObject importObject, String destBucket)
            throws ScmToolsException {
        this.importObject = importObject;
        this.destBucket = destBucket;

        S3ClientManager clientManager = S3ClientManager.getInstance();
        this.srcClient = clientManager.getSrcS3Client();
        this.destClient = clientManager.getDestS3Client();
        this.destScmS3Client = clientManager.getDestScmS3Client();
    }

    @Override
    public void create() throws ScmToolsException {
        if (!importObject.isCompleted()) {
            // 迁移重试 or 同步差异 时从文件中读取 key 列表，只是用 key 来构造 importObject
            // 此时需要重新查询对象，如果查到为空的话（对象不存在，不需要再迁移）即认为这次任务是成功的
            S3ImportObject srcObject = S3Utils.getS3Object(srcClient, importObject.getBucket(),
                    importObject.isWithVersion(), importObject.getKey());
            if (srcObject == null) {
                logger.info("object not exist, bucket={}, key={}", importObject.getBucket(),
                        importObject.getKey());
                return;
            }
            importObject = srcObject;
        }

        if (importObject.isWithVersion()) {
            for (S3VersionSummary summary : importObject.getVersionSummaryList()) {
                // 删除标记，则执行删除操作
                if (summary.isDeleteMarker()) {
                    try {
                        Map<String, Object> deleteConf = new HashMap<>();
                        deleteConf.put(CommonDefine.SCM_OBJ_CREATE_TIME,
                                summary.getLastModified().getTime());
                        destScmS3Client.deleteObject(destBucket, summary.getKey(), deleteConf);
                    }
                    catch (Exception e) {
                        throw new ScmToolsException(
                                "Failed to put deleteMarker, bucket=" + summary.getBucketName()
                                        + ", key=" + summary.getKey(),
                                S3ImportExitCode.SYSTEM_ERROR, e);
                    }
                }
                else {
                    GetObjectRequest request = new GetObjectRequest(importObject.getBucket(),
                            importObject.getKey(), summary.getVersionId());
                    S3Object srcObject = null;
                    try {
                        srcObject = S3Utils.getObject(srcClient, request);
                        S3Utils.putObject(destClient, destBucket, srcObject);
                    }
                    finally {
                        ScmCommon.closeResource(srcObject);
                    }
                }
            }
        }
        else {
            GetObjectRequest request = new GetObjectRequest(importObject.getBucket(),
                    importObject.getKey());
            S3Object srcObject = null;
            try {
                srcObject = S3Utils.getObject(srcClient, request);
                S3Utils.putObject(destClient, destBucket, srcObject);
            }
            finally {
                ScmCommon.closeResource(srcObject);
            }
        }
        logger.info("Migrate object success, bucket={}, destBucket={}, key={}",
                importObject.getBucket(), destBucket, importObject.getKey());
    }

    @Override
    public void delete() {
        S3ImportObject destObject = S3Utils.getS3Object(destClient, destBucket,
                importObject.isWithVersion(), importObject.getKey());
        if (destObject == null) {
            return;
        }

        if (destObject.isWithVersion()) {
            for (S3VersionSummary summary : destObject.getVersionSummaryList()) {
                DeleteVersionRequest request = new DeleteVersionRequest(destBucket,
                        summary.getKey(), summary.getVersionId());
                S3Utils.deleteVersion(destClient, request);
            }
        }
        else {
            DeleteObjectRequest request = new DeleteObjectRequest(destBucket, destObject.getKey());
            S3Utils.deleteObject(destClient, request);
        }
        logger.info("Delete object success, dest_bucket={}, key={}", destBucket,
                destObject.getKey());
    }
}
