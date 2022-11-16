package com.sequoiacm.contentserver.pipeline.file.bucket;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateFileMetaBucketFilter implements Filter<UpdateFileMetaContext> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateFileMetaBucketFilter.class);
    @Autowired
    private BucketInfoManager bucketInfoManager;

    @Override
    public PipelineResult executionPhase(UpdateFileMetaContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());

        ScmBucket fileCurrentBucket = getFileBucket(
                context.getCurrentLatestVersion().getBucketId());
        if (fileCurrentBucket == null) {
            // 文件不在桶下，检查 attachToBucketUpdater ，允许关联文件至桶下，关联的同时允许改名，若文件没有etag，需要生成每个版本的etag
            // updater
            FileMetaUpdater attachToBucketUpdater = context
                    .getFirstFileMetaUpdater(FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
            if (attachToBucketUpdater == null) {
                return PipelineResult.SUCCESS;
            }
            Long attachToBucketId = (Long) attachToBucketUpdater.getValue();
            if (attachToBucketId == null) {
                return PipelineResult.SUCCESS;
            }

            ScmBucket attachToBucket = bucketInfoManager.getBucketById(attachToBucketId);

            if (attachToBucket == null) {
                throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                        "failed to update file bucket id, bucket not exist: ws=" + context.getWs()
                                + ", fileId=" + context.getFileId() + ", bucket="
                                + attachToBucketId);
            }

            if (attachToBucket.getVersionStatus() == ScmBucketVersionStatus.Disabled) {
                if (!context.getCurrentLatestVersion().isNullVersion()) {
                    throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                            "file is not null version, please enable versioning on the bucket:ws="
                                    + wsInfo.getName() + ", bucket=" + attachToBucket.getName()
                                    + ", versionStatus=" + attachToBucket.getVersionStatus());
                }
            }
            FileMetaUpdater latestVersionEtagUpdater = null;
            if (context.getCurrentLatestVersion().getEtag() == null) {
                latestVersionEtagUpdater = genEtagUpdater(context.getCurrentLatestVersion());
                context.addFileMetaUpdater(latestVersionEtagUpdater);
            }

            if (!context.getCurrentLatestVersion().isFirstVersion()) {
                setHistoryFileEtagUpdater(context, wsInfo);
            }


            FileMetaUpdater renameUpdater = context
                    .getFirstFileMetaUpdater(FieldName.FIELD_CLFILE_NAME);
            if (renameUpdater != null) {
                if (!context.getCurrentLatestVersion().getName().equals(renameUpdater.getValue())) {
                    context.addFileMetaUpdater(new FileMetaUpdater(
                            FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                                    + FieldName.FIELD_CLFILE_FILE_EXT_NAME_BEFORE_ATTACH,
                            context.getCurrentLatestVersion().getName()));
                }
            }

            createBucketRelation(context, wsInfo, attachToBucket, latestVersionEtagUpdater,
                    context.getFirstFileMetaUpdater(FieldName.FIELD_CLFILE_NAME));
            return PipelineResult.SUCCESS;
        }

        // 文件已经在桶下，不允许改名、不允许关联到新桶，允许取消桶关联attachToBucketUpdater(bucketId=null)
        if (context.isContainUpdateKey(FieldName.FIELD_CLFILE_NAME)) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "can not rename file because the file in bucket:ws=" + wsInfo.getName()
                            + ", fileId=" + context.getFileId() + ", bucketId="
                            + context.getCurrentLatestVersion().getBucketId());
        }

        FileMetaUpdater attachToBucketUpdater = context
                .getFirstFileMetaUpdater(FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (attachToBucketUpdater != null) {
            Long attachToBucketId = (Long) attachToBucketUpdater.getValue();
            if (attachToBucketId == null) {
                deleteBucketRelation(context, fileCurrentBucket);
                return PipelineResult.SUCCESS;
            }
            if (attachToBucketId == fileCurrentBucket.getId()) {
                logger.info("file already in the specified bucket: ws={}, fileId={}, bucket={}",
                        context.getWs(), context.getFileId(), fileCurrentBucket.getName());
                return PipelineResult.SUCCESS;
            }
            throw new ScmServerException(ScmError.FILE_IN_ANOTHER_BUCKET,
                    "file already in another bucket:ws=" + wsInfo.getName() + ", bucketName="
                            + fileCurrentBucket.getName() + ", fileId=" + context.getFileId());
        }

        updateBucketRelation(context, fileCurrentBucket);

        return PipelineResult.SUCCESS;
    }

    private void deleteBucketRelation(UpdateFileMetaContext context, ScmBucket fileCurrentBucket)
            throws ScmServerException {
        try {
            fileCurrentBucket.getFileTableAccessor(context.getTransactionContext())
                    .delete(new BasicBSONObject(FieldName.BucketFile.FILE_NAME,
                            context.getCurrentLatestVersion().getName()));
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to detach file from bucket, delete bucket relation failed: bucket="
                            + fileCurrentBucket.getName() + ", ws="
                            + fileCurrentBucket.getWorkspace() + ", fileName="
                            + context.getCurrentLatestVersion().getName() + ", fileId="
                            + context.getFileId(),
                    e);
        }
    }

    private void updateBucketRelation(UpdateFileMetaContext context, ScmBucket fileCurrentBucket)
            throws ScmServerException {
        BSONObject bucketRelUpdater = new BasicBSONObject();
        for (FileMetaUpdater fileMetaUpdater : context.getFileMetaUpdaterList()) {
            if (!fileMetaUpdater.isGlobal() && !isLatestVersion(fileMetaUpdater,
                    context.getCurrentLatestVersion().getMajorVersion(),
                    context.getCurrentLatestVersion().getMinorVersion())) {
                continue;
            }
            String bucketFileMappingField = ScmMetaSourceHelper
                    .getBucketFileMappingField(fileMetaUpdater.getKey());
            if (bucketFileMappingField != null) {
                bucketRelUpdater.put(bucketFileMappingField, fileMetaUpdater.getValue());
            }
        }
        if (!bucketRelUpdater.isEmpty()) {
            try {
                fileCurrentBucket.getFileTableAccessor(context.getTransactionContext())
                        .upsert(new BasicBSONObject(FieldName.BucketFile.FILE_NAME,
                                context.getCurrentLatestVersion().getName()),
                                new BasicBSONObject("$set", bucketRelUpdater));
            }
            catch (ScmMetasourceException e) {
                throw new ScmServerException(e.getScmError(),
                        "failed  update bucket relation: ws=" + context.getWs() + ", bucket="
                                + fileCurrentBucket.getName() + ", fileName="
                                + context.getCurrentLatestVersion().getName() + ", fileId="
                                + context.getCurrentLatestVersion().getId(),
                        e);
            }
        }
    }

    private void createBucketRelation(UpdateFileMetaContext context, ScmWorkspaceInfo wsInfo,
            ScmBucket attachToBucket, FileMetaUpdater latestVersionEtagUpdater,
            FileMetaUpdater renameUpdater)
            throws ScmServerException {
        try {
            BSONObject bucketRelRecord = ScmFileOperateUtils
                    .createBucketFileRel(context.getCurrentLatestVersion().toBSONObject());
            if (latestVersionEtagUpdater != null) {
                bucketRelRecord.put(FieldName.BucketFile.FILE_ETAG,
                        latestVersionEtagUpdater.getValue());
            }
            if (renameUpdater != null) {
                bucketRelRecord.put(FieldName.BucketFile.FILE_NAME, renameUpdater.getValue());
            }
            attachToBucket.getFileTableAccessor(context.getTransactionContext())
                    .insert(bucketRelRecord);
        }
        catch (ScmMetasourceException e) {
            if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                throw new ScmServerException(ScmError.FILE_EXIST,
                        "the bucket already contain a file with same name: bucket="
                                + attachToBucket.getName() + ", ws=" + wsInfo.getName()
                                + ", fileName=" + context.getCurrentLatestVersion().getName(),
                        e);
            }
            throw new ScmServerException(e.getScmError(),
                    "failed to update file bucket id, create bucket relation failed: bucket="
                            + attachToBucket.getName() + ", ws=" + wsInfo.getName() + ", fileName="
                            + context.getCurrentLatestVersion().getName() + ", fileId="
                            + context.getFileId(),
                    e);
        }
    }

    private void setHistoryFileEtagUpdater(UpdateFileMetaContext context, ScmWorkspaceInfo wsInfo)
            throws ScmServerException {
        boolean allHistoryNeedUpdate = true;

        MetaCursor cursor = null;
        try {
            BasicBSONObject matcher = new BasicBSONObject();
            ScmMetaSourceHelper.addFileIdAndCreateMonth(matcher, context.getFileId());
            cursor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null)
                    .query(matcher, null);
            while (cursor.hasNext()) {
                BSONObject historyRecord = cursor.getNext();
                FileMeta fileMeta = FileMeta.fromRecord(historyRecord);
                if (fileMeta.getEtag() == null) {
                    FileMetaUpdater etagUpdater = genEtagUpdater(fileMeta);
                    context.addFileMetaUpdater(etagUpdater);
                }
                else {
                    allHistoryNeedUpdate = false;
                }
            }
            context.setContainAllHistoryVersionUpdater(allHistoryNeedUpdate);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to query file history version: ws=" + wsInfo.getName() + ", fileId="
                            + context.getFileId(),
                    e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean isLatestVersion(FileMetaUpdater fileMetaUpdater, int latestMajorVersion,
            int latestMinorVersion) {
        if (fileMetaUpdater.getMajorVersion() == -1 && fileMetaUpdater.getMinorVersion() == -1) {
            return true;
        }

        if (fileMetaUpdater.getMinorVersion() == latestMinorVersion
                && fileMetaUpdater.getMajorVersion() == latestMajorVersion) {
            return true;
        }
        return false;
    }

    private ScmBucket getFileBucket(Long bucketId) throws ScmServerException {
        if (bucketId == null) {
            return null;
        }
        return bucketInfoManager.getBucketById(bucketId);
    }

    private FileMetaUpdater genEtagUpdater(FileMeta fileMeta) {
        String etag = null;
        String md5 = fileMeta.getMd5();
        if (md5 != null) {
            etag = SignUtil.toHex(md5);
        }
        else {
            // 被关联的文件没有md5属性，这里按分段上传形成的对象 ETAG 格式（这个 ETAG 不代表文件 MD5），造一个 ETAG 给这个文件
            String dataId = fileMeta.getDataId();
            etag = SignUtil.calcHexMd5(dataId) + "-1";
        }
        return new FileMetaUpdater(FieldName.FIELD_CLFILE_FILE_ETAG, etag,
                fileMeta.getMajorVersion(), fileMeta.getMinorVersion());
    }
}
