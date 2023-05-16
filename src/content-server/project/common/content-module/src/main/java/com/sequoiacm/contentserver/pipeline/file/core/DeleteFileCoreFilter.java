package com.sequoiacm.contentserver.pipeline.file.core;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileContext;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileCoreFilter implements Filter<DeleteFileContext> {
    @Autowired
    private FileMetaFactory fileMetaFactory;

    @Override
    public PipelineResult executionPhase(DeleteFileContext context) throws ScmServerException {
        // 删除最新表、历史表记录
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());

        MetaCursor cursor = null;
        try {
            BSONObject latestVersion = contentModule
                    .getMetaService().getMetaSource().getFileAccessor(wsInfo.getMetaLocation(),
                            wsInfo.getName(), context.getTransactionContext())
                    .delete(context.getFileId(), -1, -1);
            if (latestVersion == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND, "file not found: ws="
                        + wsInfo.getName() + ", fileId=" + context.getFileId());
            }
            context.setDeletedLatestVersion(
                    fileMetaFactory.createFileMetaByRecord(wsInfo.getName(), latestVersion));

            if (context.getDeletedLatestVersion().isFirstVersion()) {
                return PipelineResult.success();
            }
            cursor = contentModule.getMetaService().getMetaSource()
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                            context.getTransactionContext())
                    .queryAndDeleteWithCursor(context.getFileId(), latestVersion, null, null);
            while (cursor.hasNext()) {
                context.getDeletedHistoryVersions().add(
                        fileMetaFactory.createFileMetaByRecord(wsInfo.getName(), cursor.getNext()));
            }
            return PipelineResult.success();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to delete file: ws="
                    + wsInfo.getName() + ", fileId=" + context.getFileId(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
