package com.sequoiacm.contentserver.pipeline.file.batch;

import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileContext;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.springframework.stereotype.Component;

@Component
public class OverwriteFileBatchFilter implements Filter<OverwriteFileContext> {
    @Override
    public PipelineResult executionPhase(OverwriteFileContext context) throws ScmServerException {
        if (!context.isOverwrittenFileConflict()) {
            return PipelineResult.success();
        }
        if (context.getOverwrittenFile().getBatchId() == null
                || context.getOverwrittenFile().getBatchId().isEmpty()) {
            return PipelineResult.success();
        }

        // 被覆盖文件与正在创建的文件冲突，且被覆盖的文件处于批次下，解除被覆盖文件的批次关系
        try {
            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                    .getWorkspaceInfoCheckExist(context.getWs());
            String createMonth = ScmSystemUtils.getCreateMonthFromBatchId(wsInfo,
                    context.getOverwrittenFile().getBatchId());
            ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getBatchAccessor(wsInfo.getName(), context.getTransactionContext())
                    .detachFile(context.getOverwrittenFile().getBatchId(), createMonth,
                            context.getOverwrittenFile().getId(), context.getFileMeta().getUser());
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to overwrite file, detach conflict file from batch failed: ws="
                            + context.getWs() + ",conflictFile=" + context.getOverwrittenFile().getId()
                            + ", batch=" + context.getOverwrittenFile().getBatchId(),
                    e);
        }
        return PipelineResult.success();
    }
}
