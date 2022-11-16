package com.sequoiacm.contentserver.pipeline.file.batch;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaBatchAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.springframework.stereotype.Component;

@Component
public class UpdateFileMetaBatchFilter implements Filter<UpdateFileMetaContext> {
    @Override
    public PipelineResult executionPhase(UpdateFileMetaContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());
        String currentBatchId = context.getCurrentLatestVersion().getBatchId();

        if (currentBatchId == null || currentBatchId.isEmpty()) {
            // 文件已经不处于批次下，检查是否有updater想要将文件关联至某个批次
            FileMetaUpdater batchIdUpdater = context
                    .getFirstFileMetaUpdater(FieldName.FIELD_CLFILE_BATCH_ID);
            if (batchIdUpdater == null) {
                return PipelineResult.SUCCESS;
            }
            String attachToBatchId = (String) batchIdUpdater.getValue();
            if (attachToBatchId == null) {
                return PipelineResult.SUCCESS;
            }
            attachToBatch(context, contentModule, wsInfo, attachToBatchId);
            return PipelineResult.SUCCESS;
        }

        // 文件已经处于批次下，检查是否有 updater（attachBatchId==null） 想要将文件取消关联（但是不允许关联到新的批次）
        FileMetaUpdater batchIdUpdater = context
                .getFirstFileMetaUpdater(FieldName.FIELD_CLFILE_BATCH_ID);
        if (batchIdUpdater == null) {
            return PipelineResult.SUCCESS;
        }
        String attachToBatchId = (String) batchIdUpdater.getValue();
        if (attachToBatchId != null) {
            if (attachToBatchId.equals(currentBatchId)) {
                throw new ScmServerException(ScmError.FILE_IN_SPECIFIED_BATCH,
                        "attachFile failed, the file is already in the batch: workspace="
                                + context.getWs() + ", batchId=" + attachToBatchId + ", fileId="
                                + context.getFileId());
            }
            throw new ScmServerException(ScmError.FILE_IN_ANOTHER_BUCKET,
                    "attachFile failed, the file is already in other batch: workspace="
                            + context.getWs() + ", batchId=" + currentBatchId + ", attachToBatchId="
                            + attachToBatchId + ", fileId=" + context.getFileId());
        }
        detachFromBatch(context, contentModule, wsInfo, currentBatchId);
        return PipelineResult.SUCCESS;
    }

    private void detachFromBatch(UpdateFileMetaContext context, ScmContentModule contentModule,
            ScmWorkspaceInfo wsInfo, String currentBatchId) throws ScmServerException {
        try {
            MetaBatchAccessor batchAccessor = contentModule.getMetaService().getMetaSource()
                    .getBatchAccessor(wsInfo.getName(), context.getTransactionContext());
            batchAccessor.detachFile(currentBatchId,
                    ScmSystemUtils.getCreateMonthFromBatchId(wsInfo, currentBatchId),
                    context.getFileId(), context.getUpdateUser());
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to detach batch: workspace=" + context.getWs() + ", batchId="
                            + currentBatchId + ", fileId=" + context.getFileId(),
                    e);
        }
    }

    private void attachToBatch(UpdateFileMetaContext context, ScmContentModule contentModule,
            ScmWorkspaceInfo wsInfo, String attachToBatchId) throws ScmServerException {
        try {
            MetaBatchAccessor batchAccessor = contentModule.getMetaService().getMetaSource()
                    .getBatchAccessor(wsInfo.getName(), context.getTransactionContext());
            batchAccessor.attachFile(attachToBatchId,
                    ScmSystemUtils.getCreateMonthFromBatchId(wsInfo, attachToBatchId),
                    context.getFileId(), context.getUpdateUser());
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(
                    e.getScmError(), "failed to attach batch: workspace=" + context.getWs()
                            + ", batchId=" + attachToBatchId + ", fileId=" + context.getFileId(),
                    e);
        }
    }
}
