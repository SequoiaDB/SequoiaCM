package com.sequoiacm.contentserver.pipeline.file.dir;

import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileVersionDirFilter implements Filter<DeleteFileVersionContext> {
    @Override
    public PipelineResult executionPhase(DeleteFileVersionContext context)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();

        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());
        if (!wsInfo.isEnableDirectory()) {
            return PipelineResult.success();
        }

        try {
            if (context.getLatestVersionAfterDelete() == null) {
                // 删除了最后一个版本，文件已经不存在，删除关系表记录
                contentModule.getMetaService().getMetaSource()
                        .getRelAccessor(context.getWs(), context.getTransactionContext())
                        .deleteRel(context.getFileId(),
                                context.getLatestVersionBeforeDelete().getDirId(),
                                context.getLatestVersionBeforeDelete().getName());
                return PipelineResult.success();
            }

            // 目录关系表映射的是最新的版本：
            // - 当删除了最新版本，需要更新关系表记录
            // - 当删除了历史版本，不需要处理关系表记录
            if (context.getLatestVersionBeforeDelete().getMajorVersion() == context
                    .getLatestVersionAfterDelete().getMajorVersion()
                    && context.getLatestVersionBeforeDelete().getMinorVersion() == context
                            .getLatestVersionAfterDelete().getMinorVersion()) {
                return PipelineResult.success();
            }

            BSONObject relUpdater = ScmMetaSourceHelper.createRelUpdatorByFileUpdator(
                    context.getLatestVersionAfterDelete().toBSONObject());
            MetaRelAccessor relAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource()
                    .getRelAccessor(wsInfo.getName(), context.getTransactionContext());
            relAccessor.updateRel(context.getFileId(),
                    context.getLatestVersionAfterDelete().getDirId(),
                    context.getLatestVersionAfterDelete().getName(), relUpdater);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file version, update dir relation failed: ws="
                            + context.getWs() + ", dirId="
                            + context.getLatestVersionBeforeDelete().getDirId() + ", fileId="
                            + context.getFileId() + ", fileName="
                            + context.getLatestVersionBeforeDelete().getName(),
                    e);
        }
        return PipelineResult.success();
    }
}
