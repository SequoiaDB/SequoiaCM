package com.sequoiacm.contentserver.pipeline.file.dir;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileDirFilter implements Filter<DeleteFileContext> {
    @Override
    public PipelineResult executionPhase(DeleteFileContext context) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(context.getWs());
        if (!wsInfo.isEnableDirectory()) {
            return PipelineResult.SUCCESS;
        }

        try {
            ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getRelAccessor(context.getWs(), context.getTransactionContext())
                    .deleteRel(context.getFileId(), context.getDeletedLatestVersion().getDirId(),
                            context.getDeletedLatestVersion().getName());
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file, delete dir relation failed: ws=" + context.getWs()
                            + ", fileId=" + context.getFileId() + ", dirId="
                            + context.getDeletedLatestVersion().getDirId() + ", fileName="
                            + context.getDeletedLatestVersion().getName(),
                    e);
        }

        return PipelineResult.SUCCESS;
    }
}
