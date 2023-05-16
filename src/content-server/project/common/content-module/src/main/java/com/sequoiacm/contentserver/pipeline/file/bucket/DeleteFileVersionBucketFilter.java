package com.sequoiacm.contentserver.pipeline.file.bucket;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileVersionBucketFilter implements Filter<DeleteFileVersionContext> {

    private static final Logger logger = LoggerFactory
            .getLogger(DeleteFileVersionBucketFilter.class);
    @Autowired
    public BucketInfoManager bucketInfoManager;

    @Override
    public PipelineResult executionPhase(DeleteFileVersionContext context) throws ScmServerException {
        //删除桶下文件的某个版本：
        // case1: 删除了最新版本，需要更新桶关系表记录（映射至次新版本）
        // case2: 删除了历史版本，无需处理
        // case3: 删除了最后一个版本，需要删除桶关系表记录
        if (context.getLatestVersionBeforeDelete().getBucketId() == null) {
            return PipelineResult.success();
        }

        ScmBucket bucket = bucketInfoManager
                .getBucketById(context.getLatestVersionBeforeDelete().getBucketId());
        if (bucket == null) {
            logger.warn(
                    "failed to update bucket relation, file bucket not exist: ws={}, fileId={}, bucketId={}",
                    context.getWs(), context.getFileId(),
                    context.getLatestVersionBeforeDelete().getBucketId());
            return PipelineResult.success();
        }

        try {
            if (context.getLatestVersionAfterDelete() == null) {
                // 删除了最后一个版本，文件已经不存在，删除关系表记录
                bucket.getFileTableAccessor(context.getTransactionContext())
                        .delete(new BasicBSONObject(FieldName.BucketFile.FILE_NAME,
                                context.getLatestVersionBeforeDelete().getName()));
                return PipelineResult.success();
            }

            // 桶关系表映射的是最新的版本：
            // - 当删除了最新版本，需要更新关系表记录
            // - 当删除了历史版本，不需要处理关系表记录
            if (context.getLatestVersionBeforeDelete().getMajorVersion() == context
                    .getLatestVersionAfterDelete().getMajorVersion()
                    && context.getLatestVersionBeforeDelete().getMinorVersion() == context
                            .getLatestVersionAfterDelete().getMinorVersion()) {
                return PipelineResult.success();
            }

            BSONObject bucketFileUpdater = ScmMetaSourceHelper.createBucketFileUpdatorByFileUpdator(
                    context.getLatestVersionAfterDelete().toRecordBSON());
            MetaAccessor bucketFileAccessor = bucket
                    .getFileTableAccessor(context.getTransactionContext());
            bucketFileAccessor.update(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME,
                            context.getLatestVersionBeforeDelete().getName()),
                    new BasicBSONObject(ScmMetaSourceHelper.SEQUOIADB_MODIFIER_SET,
                            bucketFileUpdater));

        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file version, update bucket relation failed: ws="
                            + context.getWs() + ", bucketName=" + bucket.getName() + ", fileId="
                            + context.getFileId() + ", fileName="
                            + context.getLatestVersionBeforeDelete().getName(),
                    e);
        }
        return PipelineResult.success();
    }
}
