package com.sequoiacm.contentserver.pipeline.file.dir;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.stereotype.Component;

@Component
public class UpdateFileMetaDirFilter implements Filter<UpdateFileMetaContext> {
    @Override
    public PipelineResult executionPhase(UpdateFileMetaContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());

        if (!wsInfo.isEnableDirectory()) {
            if (context.isContainUpdateKey(FieldName.FIELD_CLFILE_DIRECTORY_ID)) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "failed to move file: ws=" + wsInfo.getName() + ", fileId="
                                + context.getFileId());
            }
            return PipelineResult.success();
        }

        BSONObject relUpdater = new BasicBSONObject();
        for (FileMetaUpdater fileMetaUpdater : context.getFileMetaUpdaterList()) {
            if (!fileMetaUpdater.isGlobal() && !isLatestVersion(fileMetaUpdater,
                    context.getCurrentLatestVersion().getMajorVersion(),
                    context.getCurrentLatestVersion().getMinorVersion())) {
                continue;
            }
            fileMetaUpdater.injectDirRelationUpdater(relUpdater);
        }

        if (relUpdater.isEmpty()) {
            return PipelineResult.success();
        }

        MetaRelAccessor relAccessor = contentModule.getMetaService().getMetaSource()
                .getRelAccessor(wsInfo.getName(), context.getTransactionContext());
        String oldDirId = context.getCurrentLatestVersion().getDirId();
        String oldFileName = context.getCurrentLatestVersion().getName();
        try {
            relAccessor.updateRel(context.getFileId(), oldDirId, oldFileName, relUpdater);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to update dir relation table: ws="
                    + wsInfo.getName() + ", fileId=" + context.getFileId(), e);
        }

        return PipelineResult.success();
    }

    private boolean isLatestVersion(FileMetaUpdater fileMetaUpdater, int latestMajorVersion,
            int latestMinorVersion) {
        if (fileMetaUpdater.getMajorVersion() == -1 && fileMetaUpdater.getMinorVersion() == -1) {
            return true;
        }
        if (fileMetaUpdater.getMinorVersion() == latestMinorVersion
                && fileMetaUpdater.getMajorVersion() == latestMajorVersion) {
            return true;
        }
        return false;
    }
}
