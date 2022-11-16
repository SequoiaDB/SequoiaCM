package com.sequoiacm.contentserver.pipeline.file.batch;

import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileVersionBatchFilter implements Filter<DeleteFileVersionContext> {
    @Override
    public PipelineResult executionPhase(DeleteFileVersionContext context) throws ScmServerException {
        // 文件不在批次下不处理
        if (context.getLatestVersionBeforeDelete().getBatchId() == null
                || context.getLatestVersionBeforeDelete().getBatchId().trim().isEmpty()) {
            return PipelineResult.SUCCESS;
        }

        // 文件处于批次下不允许删除最后一个版本
        if (context.getLatestVersionAfterDelete() == null) {
            throw new ScmServerException(ScmError.FILE_IN_ANOTHER_BATCH,
                    "file belongs to a batch, detach the relationship before delete the last version:"
                            + "workspace=" + context.getWs() + ",file=" + context.getFileId()
                            + ",batch=" + context.getLatestVersionBeforeDelete().getBatchId());
        }

        return PipelineResult.SUCCESS;
    }
}
