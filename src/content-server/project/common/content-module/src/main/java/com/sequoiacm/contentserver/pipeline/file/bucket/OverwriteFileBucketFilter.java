package com.sequoiacm.contentserver.pipeline.file.bucket;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OverwriteFileBucketFilter implements Filter<OverwriteFileContext> {
    @Autowired
    private CreateFileBucketFilter createFileBucketFilter;

    @Autowired
    private BucketInfoManager bucketInfoManager;

    @Override
    public void preparePhase(OverwriteFileContext context) throws ScmServerException {
        if (context.isOverwrittenFileConflict()) {
            return;
        }

        // 检查预期被覆盖的文件是否与正在创建的文件在桶下冲突
        FileMeta overwrittenFile = context.getOverwrittenFile();
        if (overwrittenFile == null) {
            return;
        }

        if (isConflictInBucket(context.getOverwrittenFile(), context.getFileMeta())) {
            context.setOverwrittenFileConflict(true);
        }
    }

    @Override
    public PipelineResult executionPhase(OverwriteFileContext context) throws ScmServerException {
        FileMeta overwrittenFile = context.getOverwrittenFile();
        if (overwrittenFile == null) {
            return createFileBucketFilter.executionPhase(context);
        }

        try {
            // 若预期被覆盖的文件与正在创建的文件冲突，则删除冲突文件的桶关系表记录
            if (context.isOverwrittenFileConflict()) {
                deleteBucketRelation(overwrittenFile, context.getTransactionContext());
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete conflict file bucket relation: bucketId="
                            + overwrittenFile.getBucketId() + ", fileName=" + overwrittenFile.getName());
        }

        return createFileBucketFilter.executionPhase(context);
    }

    private boolean isConflictInBucket(FileMeta f1, FileMeta f2) {
        return f2.getBucketId() != null && f2.getBucketId().equals(f1.getBucketId())
                && f2.getName().equals(f1.getName());
    }

    private void deleteBucketRelation(FileMeta file, TransactionContext trans)
            throws ScmServerException, ScmMetasourceException {
        if (file.getBucketId() != null) {
            ScmBucket bucket = bucketInfoManager.getBucketById(file.getBucketId());
            if (bucket != null) {
                bucket.getFileTableAccessor(trans).delete(
                        new BasicBSONObject(FieldName.BucketFile.FILE_NAME, file.getName()));
            }
        }
    }
}
