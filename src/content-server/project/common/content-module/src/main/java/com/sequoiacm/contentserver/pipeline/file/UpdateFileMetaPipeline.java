package com.sequoiacm.contentserver.pipeline.file;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaContext;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.HashSet;
import java.util.Set;

public class UpdateFileMetaPipeline extends Pipeline<UpdateFileMetaContext> {

    @Override
    void preInvokeFilter(UpdateFileMetaContext context) throws ScmServerException {
        super.preInvokeFilter(context);
        if (context.getUpdateUser() != null || context.getUpdateTime() != null) {
            addUserAndTimeUpdater(context);
        }

        if (context.getCurrentLatestVersion() == null) {
            ScmContentModule contentModule = ScmContentModule.getInstance();
            try {
                BSONObject matcher = new BasicBSONObject();
                SequoiadbHelper.addFileIdAndCreateMonth(matcher, context.getFileId());
                BSONObject record = contentModule.getMetaService().getMetaSource()
                        .getFileAccessor(contentModule.getWorkspaceInfoCheckExist(context.getWs())
                                .getMetaLocation(), context.getWs(), null)
                        .queryOne(matcher, null, null);
                if (record == null) {
                    throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                            "failed to update file, file not exist: ws=" + context.getWs()
                                    + ", fileId=" + context.getFileId());
                }
                context.setCurrentLatestVersion(FileMeta.fromRecord(record));
            }
            catch (ScmMetasourceException e) {
                throw new ScmServerException(e.getScmError(),
                        "failed to update file, query file failed: ws" + context.getWs()
                                + ", fileId=" + context.getFileId(),
                        e);
            }
        }
    }

    @Override
    void postInvokeFilter(PipelineResult pipelineResult, UpdateFileMetaContext context)
            throws ScmServerException {
        super.postInvokeFilter(pipelineResult, context);
        if (pipelineResult.getStatus() != PipelineResult.Status.SUCCESS) {
            return;
        }
        if (context.getExpectUpdatedFileMeta() == null) {
            throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                    "failed to update file, the specified version not found: ws=" + context.getWs()
                            + ", fileId=" + context.getFileId() + ", expectVersion="
                            + context.getExpectVersion());
        }

    }

    private void addUserAndTimeUpdater(UpdateFileMetaContext context) throws ScmServerException {
        if (context.isHasUserFieldUpdater()) {
            if (context.getUpdateUser() == null || context.getUpdateTime() == null) {
                throw new ScmServerException(ScmError.SYSTEM_ERROR,
                        "failed to update file, missing update user or update time for update user field: ws"
                                + context.getWs() + ", fileId=" + context.getFileId());
            }
        }
        else {
            if (context.getUpdateUser() != null || context.getUpdateTime() != null) {
                throw new ScmServerException(ScmError.SYSTEM_ERROR,
                        "failed to update file, can not specified update user or update time for update system filed: ws"
                                + context.getWs() + ", fileId=" + context.getFileId());
            }
        }

        if (context.isHasGlobalUpdater()) {
            if (context.getUpdateUser() != null) {
                context.addFileMetaUpdater(
                        new FileMetaUpdater(FieldName.FIELD_CLFILE_INNER_UPDATE_USER,
                                context.getUpdateUser(), true, -1, -1));
            }
            if (context.getUpdateTime() != null) {
                context.addFileMetaUpdater(
                        new FileMetaUpdater(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME,
                                context.getUpdateTime().getTime(), true, -1, -1));
            }
            return;
        }
        Set<ScmVersion> updatedVersions = new HashSet<>();
        for (FileMetaUpdater updater : context.getFileMetaUpdaterList()) {
            updatedVersions
                    .add(new ScmVersion(updater.getMajorVersion(), updater.getMinorVersion()));
        }

        for (ScmVersion version : updatedVersions) {
            if (context.getUpdateUser() != null) {
                context.addFileMetaUpdater(new FileMetaUpdater(
                        FieldName.FIELD_CLFILE_INNER_UPDATE_USER, context.getUpdateUser(), false,
                        version.getMajorVersion(), version.getMinorVersion()));
            }
            if (context.getUpdateTime() != null) {
                context.addFileMetaUpdater(new FileMetaUpdater(
                        FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, context.getUpdateTime().getTime(),
                        false, version.getMajorVersion(), version.getMinorVersion()));
            }
        }

    }
}
