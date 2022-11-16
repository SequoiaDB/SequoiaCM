package com.sequoiacm.contentserver.pipeline.file.dir;

import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.springframework.stereotype.Component;

@Component
public class AddFileVersionDirFilter implements Filter<AddFileVersionContext> {
    @Override
    public PipelineResult executionPhase(AddFileVersionContext context) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(context.getWs());
        if (!wsInfo.isEnableDirectory()) {
            return PipelineResult.SUCCESS;
        }

        BSONObject relUpdater = ScmMetaSourceHelper
                .createRelUpdatorByFileUpdator(context.getNewVersion().toBSONObject());
        MetaRelAccessor relAccessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource().getRelAccessor(wsInfo.getName(), context.getTransactionContext());

        try {
            relAccessor.updateRel(context.getFileId(), context.getNewVersion().getDirId(),
                    context.getNewVersion().getName(), relUpdater);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to update dir relation: ws=" + context.getWs() + ", fileId="
                            + context.getFileId() + ", dirId=" + context.getNewVersion().getDirId()
                            + ", fileName=" + context.getNewVersion().getName(),
                    e);
        }
        return PipelineResult.SUCCESS;
    }
}
