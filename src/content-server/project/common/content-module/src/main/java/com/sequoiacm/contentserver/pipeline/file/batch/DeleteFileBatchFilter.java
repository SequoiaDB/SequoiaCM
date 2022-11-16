package com.sequoiacm.contentserver.pipeline.file.batch;

import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileBatchFilter implements Filter<DeleteFileContext> {
    @Override
    public PipelineResult executionPhase(DeleteFileContext context) throws ScmServerException {
        // 文件在批次下不允许删除
        if (context.getDeletedLatestVersion().getBatchId() != null
                && !context.getDeletedLatestVersion().getBatchId().trim().isEmpty()) {
            throw new ScmServerException(ScmError.FILE_IN_ANOTHER_BATCH,
                    "file belongs to a batch, detach the relationship before deleting it:"
                            + "workspace=" + context.getWs() + ",file=" + context.getFileId()
                            + ",batch=" + context.getDeletedLatestVersion().getBatchId());
        }

        return PipelineResult.SUCCESS;
    }
}
