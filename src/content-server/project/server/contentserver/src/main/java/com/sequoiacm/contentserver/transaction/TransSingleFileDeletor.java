package com.sequoiacm.contentserver.transaction;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;

public class TransSingleFileDeletor extends ScmTransBase {
    private static final Logger logger = LoggerFactory.getLogger(TransSingleFileDeletor.class);

    private ScmWorkspaceInfo wsInfo = null;
    private String workspaceName = null;
    private String fileID = null;
    private int siteID = -1;
    private int majorVersion = -1;
    private int minorVersion = -1;
    private String user = null;
    private String transID = null;
    private ScmContentServer contentServer = ScmContentServer.getInstance();

    private final int type = ServiceDefine.TransType.DELETING_SINGLE_FILE;

    // private ScmLock lock = null;

    // for normal process
    public TransSingleFileDeletor(int siteID, ScmWorkspaceInfo wsInfo, String user, String fileID,
            int majorVersion, int minorVersion) throws ScmSystemException {
        this.siteID = siteID;
        this.user = user;
        this.wsInfo = wsInfo;
        workspaceName = wsInfo.getName();
        this.fileID = fileID;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;

        transID = ScmIdGenerator.TransactionId.get();
    }

    // for roll back
    public TransSingleFileDeletor(int siteID, ScmWorkspaceInfo wsInfo, BSONObject transRecord) {
        this.siteID = siteID;
        this.user = (String) transRecord.get(FieldName.FIELD_CLTRANS_USER);
        this.wsInfo = wsInfo;
        workspaceName = wsInfo.getName();
        transID = (String) transRecord.get(FieldName.FIELD_CLTRANS_ID);
        fileID = (String) transRecord.get(FieldName.FIELD_CLTRANS_FILEID);
        majorVersion = (int) transRecord.get(FieldName.FIELD_CLTRANS_MAJORVERSION);
        minorVersion = (int) transRecord.get(FieldName.FIELD_CLTRANS_MINORVERSION);
    }

    @Override
    public int getSiteID() {
        return siteID;
    }

    @Override
    public String getWorkspaceName() {
        return workspaceName;
    }

    public int getType() {
        return type;
    }

    @Override
    public String getTransID() {
        return transID;
    }

    public String getUser() {
        return user;
    }

    private void removeFileInfo() throws ScmServerException {
        contentServer.deleteCurrentFile(wsInfo, fileID, majorVersion, minorVersion);
    }

    private void saveFileHistoryInfo() throws ScmServerException {
        BSONObject file = contentServer.getMetaService().getFileInfo(wsInfo.getMetaLocation(),
                wsInfo.getName(), fileID, majorVersion, minorVersion);

        if (null == file) {
            throw new ScmFileNotFoundException("file is unexist:workspace=" + workspaceName
                    + ",file=" + fileID + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        // TODO:
        // file = SequoiadbHelper.currentFileToHistory(file,
        // ServiceDefine.FileHistoryFlag.DELETED,
        // user, new Date());
        // contentServer.insertHistoryFile(workspaceName, file);
    }

    private void saveTransLog(ScmContentServer contentServer) throws ScmServerException {
        BSONObject transRecord = new BasicBSONObject();
        transRecord.put(FieldName.FIELD_CLTRANS_ID, getTransID());
        transRecord.put(FieldName.FIELD_CLTRANS_TYPE, getType());
        transRecord.put(FieldName.FIELD_CLTRANS_INDEX, 0);
        transRecord.put(FieldName.FIELD_CLTRANS_USER, getUser());
        transRecord.put(FieldName.FIELD_CLTRANS_CREATETIME, new Date().getTime());
        transRecord.put(FieldName.FIELD_CLTRANS_FILEID, fileID);
        transRecord.put(FieldName.FIELD_CLTRANS_MAJORVERSION, majorVersion);
        transRecord.put(FieldName.FIELD_CLTRANS_MINORVERSION, minorVersion);
        contentServer.insertTransLog(getWorkspaceName(), transRecord);
    }

    private void markFileInTrans(ScmContentServer contentServer) throws ScmServerException {
        ScmMetaService sms = contentServer.getMetaService();
        try {
            boolean isUpdated = sms.updateTransId(wsInfo, fileID, majorVersion, minorVersion,
                    ServiceDefine.FileStatus.DELETING, getTransID());
            if (!isUpdated) {
                throw new ScmFileNotFoundException("file is unexist:workspace="
                        + getWorkspaceName() + ",file=" + fileID + ",version="
                        + ScmSystemUtils.getVersionStr(majorVersion, minorVersion) + ",transID="
                        + "\"\"");
            }
        }
        catch (ScmServerException e) {
            throw e;
        }
    }

    private void unmarkFileIntrans(ScmContentServer contentServer, String fileID, int majorVersion,
            int minorVersion) throws ScmServerException {
        logger.debug("set current status:" + FieldName.FIELD_CLFILE_EXTRA_STATUS + "="
                + ServiceDefine.FileStatus.NORMAL + "," + FieldName.FIELD_CLFILE_EXTRA_TRANS_ID
                + "=\"\"");

        logger.info("updating file:wsName=" + workspaceName + ",fileId=" + fileID + ",version="
                + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        contentServer.getMetaService().unmarkTransIdFromFile(wsInfo, fileID, majorVersion,
                minorVersion, ServiceDefine.FileStatus.NORMAL);
    }

    private void removeFileHistoryInfo(ScmContentServer contentServer) throws ScmServerException {
        // try {
        // contentServer.deleteHistoryFile(getWorkspaceName(), fileID,
        // majorVersion, minorVersion);
        // }
        // catch (ScmServerException e) {
        // logger.error("delete history file failed:fileID=" + fileID +
        // ",version="
        // + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        // throw e;
        // }
    }

    @Override
    protected void _lock() throws ScmServerException {
        // TODO:
        // boolean isLocked = false;
        // ScmZKReadWriteLock rw = new ScmZKReadWriteLock(fileID,
        // CommonDefine.InstanceType.FILE_STR);
        // lock = rw.writeLock();
        // try {
        // isLocked = lock.lock(-1);
        // }
        // catch (Exception e) {
        // logger.error( "lock failed:fileID=" + fileID + ",version="
        // + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        // throw e;
        // }
        //
        // if (!isLocked) {
        // throw new ScmLockException(
        // "lock file failed:fileID=" + fileID + ",version="
        // + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        // }
    }

    @Override
    protected void _unlock() {
        // if (null != lock) {
        // lock.unlock();
        // }
    }

    @Override
    protected void _execute() throws ScmServerException {
        try {
            saveTransLog(contentServer);
            markFileInTrans(contentServer);
            saveFileHistoryInfo();
            removeFileInfo();

            // after remove file info. we assume remove operator have been
            // successfully executed.
            // the following code must not throws any errors.
            removeTransLog(contentServer, siteID, workspaceName, transID);
        }
        catch (Exception e) {
            logger.error("delete file failed:fileID=" + fileID + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
            throw e;
        }
    }

    @Override
    protected void _innerRollback() throws ScmServerException {
        logger.warn("start to rollback file:fileID=" + fileID + ",version="
                + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        logger.warn("start to delete history file record");
        removeFileHistoryInfo(contentServer);

        logger.warn("start to unmark file trans status");
        unmarkFileIntrans(contentServer, fileID, majorVersion, minorVersion);
        // after unmark file's transaction flag. we assume rollback is
        // success.
        logger.warn("delete translog:transID=" + transID);
        removeTransLog(contentServer, siteID, workspaceName, transID);
    }

    @Override
    protected boolean _isInTransStatus() throws ScmServerException {
        BSONObject file = contentServer.getMetaService().getFileInfo(wsInfo.getMetaLocation(),
                wsInfo.getName(), fileID, majorVersion, minorVersion);
        if (null == file) {
            // file is unexist, do not need to rollback
            logger.info("file is unexist,do not need to rollback:file=" + fileID + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
            return false;
        }

        String currentTrasnID = (String) file.get(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID);
        if (!transID.equals(currentTrasnID)) {
            logger.info("file's transaction is mismatch,do not need to rollback:file=" + fileID
                    + ",version=" + ScmSystemUtils.getVersionStr(majorVersion, minorVersion)
                    + ",transID=" + currentTrasnID + "job's transID=" + transID);
            return false;
        }

        return true;
    }
}
