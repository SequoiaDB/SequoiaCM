package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.Date;

public abstract class FileInfoUpdater {
    public BSONObject update(ScmWorkspaceInfo ws, String fileId, int majorVersion, int minorVersion,
            BSONObject updater, String updateUser) throws ScmServerException {
        ContentModuleMetaSource metasource = ScmContentModule.getInstance().getMetaService()
                .getMetaSource();
        TransactionContext trans = null;
        try {
            trans = metasource.createTransactionContext();
            trans.begin();
            BSONObject ret = update(ws, fileId, majorVersion, minorVersion, updater, updateUser,
                    trans);
            trans.commit();
            return ret;
        }
        catch (Exception e) {
            if (trans != null) {
                trans.rollback();
            }
            if (e instanceof ScmServerException) {
                throw (ScmServerException) e;
            }
            ScmError error = ScmError.SYSTEM_ERROR;
            if (e instanceof ScmMetasourceException) {
                error = ((ScmMetasourceException) e).getScmError();
            }

            throw new ScmServerException(error,
                    "failed to update file info: ws=" + ws + ", fileId=" + fileId, e);
        }
        finally {
            if (trans != null) {
                trans.close();
            }
        }
    }

    public abstract BSONObject update(ScmWorkspaceInfo ws, String fileId, int majorVersion,
            int minorVersion, BSONObject updater, String updateUser,
            TransactionContext transactionContext) throws ScmServerException;
}

class NotUnifiedInfoUpdater extends FileInfoUpdater {

    @Override
    public BSONObject update(ScmWorkspaceInfo ws, String fileId, int majorVersion, int minorVersion,
            BSONObject updater, String updateUser, TransactionContext trans)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ContentModuleMetaSource metasource = contentModule.getMetaService().getMetaSource();

        // add modify user and time
        updater.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, updateUser);
        Date updateTime = new Date();
        updater.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, updateTime.getTime());

        MetaFileHistoryAccessor historyAccessor = metasource
                .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), trans);

        try {
            if (majorVersion == -1 && minorVersion == -1) {
                ScmFileVersionHelper.updateLatestVersionAndRel(ws, fileId, updater, trans);
                return updater;
            }
            BSONObject oldLatestVersion = ScmFileVersionHelper
                    .updateLatestVersionAndRel(ws, fileId,
                            new BasicBSONObject(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion)
                                    .append(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion),
                            updater, trans);
            if (oldLatestVersion != null) {
                return updater;
            }
            BasicBSONObject matcher = new BasicBSONObject();
            ScmMetaSourceHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.append(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion)
                    .append(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            BSONObject ret = historyAccessor.updateAndReturnNew(matcher, new BasicBSONObject("$set", updater));
            if (ret == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file not found: ws=" + ws.getName() + ", fileId=" + fileId + ", version="
                                + majorVersion + "." + minorVersion);
            }
            return updater;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "update file failed: ws=" + ws.getName()
                    + ", fileId=" + fileId + ", version=" + majorVersion + "." + minorVersion, e);
        }
    }
}

class UnifiedInfoUpdater extends FileInfoUpdater {

    @Override
    public BSONObject update(ScmWorkspaceInfo ws, String fileId, int majorVersion, int minorVersion,
            BSONObject updater, String updateUser, TransactionContext trans)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ContentModuleMetaSource metasource = contentModule.getMetaService().getMetaSource();

        BasicBSONObject matcher = new BasicBSONObject();
        ScmMetaSourceHelper.addFileIdAndCreateMonth(matcher, fileId);

        // add modify user and time
        updater.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, updateUser);
        Date updateTime = new Date();
        updater.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, updateTime.getTime());

        MetaFileHistoryAccessor historyAccessor = metasource
                .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), trans);
        try {
            ScmFileVersionHelper.updateLatestVersionAndRel(ws, fileId, updater, trans);
            historyAccessor.updateAndReturnNew(matcher, new BasicBSONObject("$set", updater));
            return updater;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "update file failed: ws=" + ws.getName()
                    + ", fileId=" + fileId + ", version=" + majorVersion + "." + minorVersion, e);
        }
    }

}
