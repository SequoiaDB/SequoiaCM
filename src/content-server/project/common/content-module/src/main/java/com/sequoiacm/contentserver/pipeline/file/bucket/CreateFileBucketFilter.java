package com.sequoiacm.contentserver.pipeline.file.bucket;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileContext;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaExistException;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateFileBucketFilter implements Filter<CreateFileContext> {
    private static final Logger logger = LoggerFactory.getLogger(CreateFileBucketFilter.class);
    @Autowired
    private BucketInfoManager bucketInfoManager;

    @Override
    public PipelineResult executionPhase(CreateFileContext context) throws ScmServerException {
        // 桶下创建文件，需要根据桶的版本控制状态，调整文件的版本号，并且创建桶关系记录

        FileMeta fileMeta = context.getFileMeta();

        if (context.getFileMeta().getMd5() != null && !context.getFileMeta().getMd5().isEmpty()) {
            if (context.getFileMeta().getEtag() == null
                    || context.getFileMeta().getEtag().isEmpty()) {
                fileMeta.setEtag(SignUtil.toHex(fileMeta.getMd5()));
            }
        }

        Long bucketId = fileMeta.getBucketId();
        if (bucketId == null) {
            return PipelineResult.success();
        }

        ScmBucket bucket = bucketInfoManager.getBucketById(bucketId);
        if (bucket == null) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                    "bucket not found: bucketId=" + bucketId);
        }

        if (fileMeta.getEtag() == null || fileMeta.getEtag().isEmpty()) {
            if (!fileMeta.isDeleteMarker()) {
                throw new ScmServerException(ScmError.SYSTEM_ERROR,
                        "failed to create file: etag not found");
            }
        }

        if (bucket.getVersionStatus() != ScmBucketVersionStatus.Enabled) {
            fileMeta.setMajorVersion(CommonDefine.File.NULL_VERSION_MAJOR);
            fileMeta.setMinorVersion(CommonDefine.File.NULL_VERSION_MINOR);
            fileMeta.setVersionSerial("1.0");
        }

        BSONObject bucketRelRecord = ScmFileOperateUtils
                .createBucketFileRel(fileMeta.toRecordBSON());
        try {
            MetaAccessor bucketRelTable = bucket
                    .getFileTableAccessor(context.getTransactionContext());
            bucketRelTable.insert(bucketRelRecord);
        }
        catch (ScmMetasourceException e) {
            if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                logger.debug("bucket relation already exist: bucket={}, file={}", bucket.getName(),
                        fileMeta.getName(), e);
                String fileId = findFile(bucket, fileMeta.getName());
                if (fileId == null) {
                    // 写入关系表时索引冲突，查找冲突文件时又未找到，通知上层重新触发一次 Pipeline，同时把异常带出去，用于停止重试时提示用户
                    return PipelineResult
                            .redo(new ScmServerException(
                                    ScmError.FILE_EXIST, "file already exist: bucket="
                                            + bucket.getName() + ", fileName=" + fileMeta.getName(),
                                    e));
                }
                throw new FileMetaExistException(
                        "failed to create file, create bucket relation failed: bucket="
                                + bucket.getName() + ", fileName=" + fileMeta.getName(),
                        e, fileId);
            }
            throw new ScmServerException(e.getScmError(),
                    "failed to create file, create bucket relation failed: bucket="
                            + bucket.getName() + ", fileName=" + fileMeta.getName(),
                    e);
        }

        return PipelineResult.success();
    }

    private String findFile(ScmBucket bucket, String fileName) throws ScmServerException {
        try {
            BSONObject record = bucket.getFileTableAccessor(null).queryOne(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, fileName), null, null);
            if (record == null) {
                return null;
            }
            return BsonUtils.getStringChecked(record, FieldName.BucketFile.FILE_ID);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to query bucket relation: bucket="
                    + bucket.getName() + ", fileName=" + fileName, e);
        }
    }
}
