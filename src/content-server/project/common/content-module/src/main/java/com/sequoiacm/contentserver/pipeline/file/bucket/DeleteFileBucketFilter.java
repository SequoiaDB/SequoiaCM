package com.sequoiacm.contentserver.pipeline.file.bucket;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileBucketFilter implements Filter<DeleteFileContext> {
    @Autowired
    private BucketInfoManager bucketInfoManager;

    @Override
    public PipelineResult executionPhase(DeleteFileContext context) throws ScmServerException {
        // 桶下删除文件，需要删除桶关系
        if (context.getDeletedLatestVersion().getBucketId() == null) {
            return PipelineResult.success();
        }
        ScmBucket bucket = bucketInfoManager
                .getBucketById(context.getDeletedLatestVersion().getBucketId());
        if (bucket == null) {
            return PipelineResult.success();
        }

        try {
            bucket.getFileTableAccessor(context.getTransactionContext()).delete(new BasicBSONObject(
                    FieldName.BucketFile.FILE_NAME, context.getDeletedLatestVersion().getName()));
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file, delete bucket relation failed: ws=" + context.getWs()
                            + ", bucket=" + bucket.getName() + ", fileName="
                            + context.getDeletedLatestVersion().getName() + ", fileId="
                            + context.getDeletedLatestVersion().getId(),
                    e);
        }

        return PipelineResult.success();
    }
}
