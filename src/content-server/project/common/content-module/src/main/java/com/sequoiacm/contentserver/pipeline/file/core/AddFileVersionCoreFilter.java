package com.sequoiacm.contentserver.pipeline.file.core;

import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.AddFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.stereotype.Component;

@Component
public class AddFileVersionCoreFilter implements Filter<AddFileVersionContext> {
    @Override
    public PipelineResult executionPhase(AddFileVersionContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckExist(context.getWs());
        try {
            // 前置filter（bucket）置位该标记，表示希望新增版本的同时删除某个版本
            if (context.getShouldDeleteVersion() != null) {

                // 需要删除的是最新版本
                if (context.getCurrentLatestVersion().getMajorVersion() == context
                        .getShouldDeleteVersion().getMajorVersion()
                        && context.getCurrentLatestVersion().getMinorVersion() == context
                                .getShouldDeleteVersion().getMinorVersion()) {
                    updateLatestFile(context, ws);
                    context.setDeletedVersion(context.getCurrentLatestVersion());
                    return PipelineResult.success();
                }

                ScmFileVersionHelper.insertVersionToHistory(ws, context.getCurrentLatestVersion(),
                        context.getTransactionContext());
                updateLatestFile(context, ws);
                BSONObject deletedVersion = ScmFileVersionHelper.deleteVersionInHistory(ws,
                        context.getFileId(), context.getShouldDeleteVersion(),
                        context.getTransactionContext(),
                        context.getCurrentLatestVersion().toBSONObject());
                if (deletedVersion != null) {
                    context.setDeletedVersion(FileMeta.fromRecord(deletedVersion));
                }
                return PipelineResult.success();
            }

            ScmFileVersionHelper.insertVersionToHistory(ws, context.getCurrentLatestVersion(),
                    context.getTransactionContext());
            updateLatestFile(context, ws);
            return PipelineResult.success();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to add version: ws=" + ws.getName() + ", fileId=" + context.getFileId(),
                    e);
        }

    }

    private void updateLatestFile(AddFileVersionContext context, ScmWorkspaceInfo ws)
            throws ScmServerException, ScmMetasourceException {
        MetaFileAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                .getFileAccessor(ws.getMetaLocation(), ws.getName(),
                        context.getTransactionContext());
        BSONObject matcher = new BasicBSONObject();
        SequoiadbHelper.addFileIdAndCreateMonth(matcher, context.getFileId());
        BSONObject oldRecord = accessor.queryAndUpdate(matcher,
                new BasicBSONObject("$set", context.getNewVersion().toBSONObject()), null);
        if (oldRecord == null) {
            throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                    "failed to add file version, file not found: ws=" + ws.getName() + ", fileId="
                            + context.getFileId());
        }
    }
}
