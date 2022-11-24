package com.sequoiacm.contentserver.pipeline.file.core;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.DeleteFileVersionContext;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.Filter;
import com.sequoiacm.contentserver.pipeline.file.PipelineResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeleteFileVersionCoreFilter implements Filter<DeleteFileVersionContext> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteFileVersionCoreFilter.class);
    @Override
    public PipelineResult executionPhase(DeleteFileVersionContext context) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(context.getWs());

        BSONObject latestVersionBeforeDeleteBson = contentModule.getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), wsInfo.getName(), context.getFileId(), -1, -1);
        if (latestVersionBeforeDeleteBson == null) {
            throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                    "file version not found: ws=" + wsInfo.getName() + ", fileId="
                            + context.getFileId() + ", majorVersion=" + context.getMajorVersion()
                            + ", minorVersion=" + context.getMinorVersion());
        }

        FileMeta latestVersionBeforeDelete = FileMeta.fromRecord(latestVersionBeforeDeleteBson);

        context.setLatestVersionBeforeDelete(latestVersionBeforeDelete);

        MetaFileHistoryAccessor fileHistoryAccessor = contentModule.getMetaService().getMetaSource()
                .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                        context.getTransactionContext());
        MetaFileAccessor fileAccessor = contentModule.getMetaService().getMetaSource()
                .getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                        context.getTransactionContext());

        if (isDeleteLatestVersion(latestVersionBeforeDelete, context)) {
            // 删除最新版本：需要将次新版本移动覆盖到到最新表
            deleteLatestVersion(context, wsInfo, latestVersionBeforeDelete, fileHistoryAccessor,
                    fileAccessor);
        }
        else {
            // 删除历史版本：直接到历史表删除指定版本
            deleteHistoryVersion(context, wsInfo, latestVersionBeforeDelete, fileHistoryAccessor);
        }

        return PipelineResult.success();
    }

    private void deleteLatestVersion(DeleteFileVersionContext context, ScmWorkspaceInfo wsInfo,
            FileMeta latestVersionBeforeDelete, MetaFileHistoryAccessor fileHistoryAccessor,
            MetaFileAccessor fileAccessor) throws ScmServerException {
        try {
            context.setDeletedVersion(latestVersionBeforeDelete);
            if (latestVersionBeforeDelete.isFirstVersion()) {
                fileAccessor.delete(context.getFileId(), -1, -1);
                return;
            }

            BSONObject idMatcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(idMatcher, context.getFileId());

            // queryAndDelete 使用的排序条件需要为索引字段，所以无法使用 queryAndDelete，这里先 query 再 delete
            BSONObject latestVersionAfterDeleteBson = fileHistoryAccessor.queryOne(idMatcher,
                    new BasicBSONObject(FieldName.FIELD_CLFILE_MAJOR_VERSION, -1)
                            .append(FieldName.FIELD_CLFILE_MINOR_VERSION, -1));
            if (latestVersionAfterDeleteBson != null) {
                FileMeta latestVersionAfterDelete = FileMeta
                        .fromRecord(latestVersionAfterDeleteBson);
                fileHistoryAccessor.delete(context.getFileId(),
                        latestVersionAfterDelete.getMajorVersion(),
                        latestVersionAfterDelete.getMinorVersion());
                BSONObject oldRecord = fileAccessor.queryAndUpdate(idMatcher,
                        new BasicBSONObject("$set", latestVersionAfterDeleteBson), null);
                if (oldRecord == null) {
                    throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                            "file version not found: ws=" + wsInfo.getName() + ", fileId="
                                    + context.getFileId() + ", majorVersion="
                                    + context.getMajorVersion() + ", minorVersion="
                                    + context.getMinorVersion());
                }
                context.setLatestVersionAfterDelete(latestVersionAfterDelete);
                return;
            }
            fileAccessor.delete(context.getFileId(), -1, -1);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file version: ws=" + wsInfo.getName() + ", fileId="
                            + context.getFileId() + ", majorVersion=" + context.getMajorVersion()
                            + ", minorVersion=" + context.getMinorVersion(),
                    e);
        }
    }

    private void deleteHistoryVersion(DeleteFileVersionContext context, ScmWorkspaceInfo wsInfo,
            FileMeta latestVersionBeforeDelete, MetaFileHistoryAccessor fileHistoryAccessor)
            throws ScmServerException {
        try {
            BSONObject deletedVersion = fileHistoryAccessor.queryAndDelete(context.getFileId(),
                    latestVersionBeforeDelete.toBSONObject(),
                    new BasicBSONObject(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                            context.getMajorVersion()).append(FieldName.FIELD_CLFILE_MINOR_VERSION,
                                    context.getMinorVersion()),
                    null);
            if (deletedVersion == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file version not found: ws=" + wsInfo.getName() + ", fileId="
                                + context.getFileId() + ", majorVersion="
                                + context.getMajorVersion() + ", minorVersion="
                                + context.getMinorVersion());
            }
            context.setLatestVersionAfterDelete(latestVersionBeforeDelete);
            context.setDeletedVersion(FileMeta.fromRecord(deletedVersion));
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to delete file version: ws=" + wsInfo.getName() + ", fileId="
                            + context.getFileId() + ", majorVersion=" + context.getMajorVersion()
                            + ", minorVersion=" + context.getMinorVersion(),
                    e);
        }
    }

    private boolean isDeleteLatestVersion(FileMeta latestVersionBeforeDelete,
            DeleteFileVersionContext context) {
        if (context.getMajorVersion() == -1 && context.getMinorVersion() == -1) {
            return true;
        }
        if (context.getMajorVersion() == latestVersionBeforeDelete.getMajorVersion()
                && context.getMinorVersion() == latestVersionBeforeDelete.getMinorVersion()) {
            return true;
        }
        return false;
    }
}
