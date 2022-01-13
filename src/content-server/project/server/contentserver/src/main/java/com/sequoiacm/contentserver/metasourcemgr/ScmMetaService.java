package com.sequoiacm.contentserver.metasourcemgr;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.contentserver.cache.ScmDirPath;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.FileDeletorDao;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.model.*;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbSiteUrl;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.metasource.*;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.base.DBQuery;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScmMetaService {
    private static final Logger logger = LoggerFactory.getLogger(ScmMetaService.class);

    private int siteId;
    private ContentModuleMetaSource metasource;
    private BreakpointFileBsonConverter breakpointFileBsonConverter = new BreakpointFileBsonConverter();
    private MetadataAttrBsonConverter metadataAttrBsonConverter = new MetadataAttrBsonConverter();
    private MetadataClassBsonConverter metadataClassBsonConverter = new MetadataClassBsonConverter();

    public ScmMetaService(int siteId, ScmSiteUrl siteUrl) throws ScmServerException {
        this.siteId = siteId;
        // TODO:
        if (siteUrl instanceof SdbSiteUrl) {
            SdbSiteUrl url = (SdbSiteUrl) siteUrl;
            try {
                AuthInfo auth = ScmFilePasswordParser.parserFile(siteUrl.getPassword());
                metasource = new SdbMetaSource(url.getUrls(), url.getUser(), auth.getPassword(),
                        url.getConfig(), url.getDatasourceOption());
            }
            catch (ScmMetasourceException e) {
                throw new ScmServerException(e.getScmError(), "Failed to create SdbMetaSource", e);
            }
            catch (Exception e) {
                throw new ScmSystemException("Failed to create SdbMetaSource", e);
            }
        }
        else {
            throw new ScmOperationUnsupportedException("only support sequoiadb yet");
        }
    }

    public void insertFile(ScmWorkspaceInfo wsInfo, BSONObject file) throws ScmServerException {
        insertFile(wsInfo, file, (TransactionCallback) null);
    }

    public void insertFile(ScmWorkspaceInfo wsInfo, BSONObject file,
            TransactionCallback transactionCallback) throws ScmServerException {
        TransactionContext context = createTransactionContext();
        try {
            beginTransaction(context);
            insertFile(wsInfo, file, context);
            if (transactionCallback != null) {
                transactionCallback.beforeTransactionCommit(context);
            }
            commitTransaction(context);
        }
        catch (ScmServerException e) {
            rollbackTransaction(context);
            if (e.getError() == ScmError.FILE_TABLE_NOT_FOUND) {
                logger.debug("create table", e);
                try {
                    metasource.getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), context)
                            .createFileTable(file);
                }
                catch (Exception ex) {
                    throw new ScmServerException(ScmError.METASOURCE_ERROR,
                            "insert file failed, create file table failed:ws=" + wsInfo.getName()
                                    + ",siteId=" + siteId + ",file="
                                    + file.get(FieldName.FIELD_CLFILE_ID),
                            ex);
                }
                insertFile(wsInfo, file, transactionCallback);
                return;
            }
            throw new ScmServerException(e.getError(), "insert file failed:siteId=" + siteId
                    + ",file=" + file.get(FieldName.FIELD_CLFILE_ID), e);
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw new ScmSystemException("insert file failed:siteId=" + siteId + ",file="
                    + file.get(FieldName.FIELD_CLFILE_ID), e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    private void insertFile(ScmWorkspaceInfo wsInfo, BSONObject file, TransactionContext context)
            throws ScmServerException {
        try {
            ScmFileOperateUtils.insertFileRelForCreateFile(wsInfo, file, context);
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), context);
            fileAccessor.insert(file);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "insert file failed:siteId=" + siteId
                    + ",file=" + file.get(FieldName.FIELD_CLFILE_ID), e);
        }
        catch (Exception e) {
            throw new ScmSystemException("insert file failed:siteId=" + siteId + ",file="
                    + file.get(FieldName.FIELD_CLFILE_ID), e);
        }
    }

    public void addSiteInfoToFile(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int siteId, Date date) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            boolean isSuccess = fileAccessor.addToSiteList(fileId, majorVersion, minorVersion,
                    siteId, date);
            if (isSuccess) {
                return;
            }

            MetaFileHistoryAccessor historyAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
            historyAccessor.addToSiteList(fileId, majorVersion, minorVersion, siteId, date);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(
                    e.getScmError(), "add site info failed:fileId=" + fileId + ",majorVersion="
                            + majorVersion + ",minorVersion=" + minorVersion + ",siteId=" + siteId,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("add site info failed:fileId=" + fileId + ",majorVersion="
                    + majorVersion + ",minorVersion=" + minorVersion + ",siteId=" + siteId, e);
        }
    }

    public void deleteNullSiteFromFile(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            if (fileAccessor.deleteNullFromSiteList(fileId, majorVersion, minorVersion)) {
                return;
            }

            MetaFileHistoryAccessor historyAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
            historyAccessor.deleteNullFromSiteList(fileId, majorVersion, minorVersion);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "delete null site failed:fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("delete null site failed:fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion, e);
        }
    }

    public void updateAccessTimeInFile(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int siteId, Date date) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            if (fileAccessor.updateAccessTime(fileId, majorVersion, minorVersion, siteId, date)) {
                return;
            }
            MetaFileHistoryAccessor historyAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
            historyAccessor.updateAccessTime(fileId, majorVersion, minorVersion, siteId, date);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(
                    e.getScmError(), "update access time failed:fileId=" + fileId + ",majorVersion="
                            + majorVersion + ",minorVersion=" + minorVersion + ",siteId=" + siteId,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "update access time failed:fileId=" + fileId + ",majorVersion=" + majorVersion
                            + ",minorVersion=" + minorVersion + ",siteId=" + siteId,
                    e);
        }
    }

    public void deleteSiteFromFile(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int siteId) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            if (fileAccessor.deleteFromSiteList(fileId, majorVersion, minorVersion, siteId)) {
                return;
            }
            MetaFileHistoryAccessor historyAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
            historyAccessor.deleteFromSiteList(fileId, majorVersion, minorVersion, siteId);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "delete site from file failed:fileId=" + fileId + ",majorVersion="
                            + majorVersion + ",minorVersion=" + minorVersion + ",siteId=" + siteId,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "delete site from file failed:fileId=" + fileId + ",majorVersion="
                            + majorVersion + ",minorVersion=" + minorVersion + ",siteId=" + siteId,
                    e);
        }
    }

    public void updateFileInfo(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, BSONObject fileUpdator) throws ScmServerException {
        updateFileInfo(wsInfo, fileId, majorVersion, minorVersion, fileUpdator, null);
    }

    public boolean updateFileExternalData(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData, TransactionContext context)
            throws ScmServerException {
        try {
            BasicBSONObject matcher = new BasicBSONObject();
            SequoiadbHelper.addFileIdAndCreateMonth(matcher, fileId);
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);

            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), context);
            BSONObject newFileInfo = fileAccessor.updateFileExternalData(matcher, externalData);
            if (newFileInfo != null) {
                if (newFileInfo.get(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA) != null) {
                    return true;
                }
                BSONObject oldRec = fileAccessor.updateFileInfo(fileId, majorVersion, minorVersion,
                        new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA,
                                externalData));
                if (oldRec != null) {
                    return true;
                }
            }
            MetaFileHistoryAccessor historyFileAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), context);
            newFileInfo = historyFileAccessor.updateFileExternalData(matcher, externalData);
            if (newFileInfo != null) {
                if (newFileInfo.get(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA) != null) {
                    return true;
                }
                BSONObject oldRec = historyFileAccessor.updateFileInfo(fileId, majorVersion,
                        minorVersion, new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA,
                                externalData));
                if (oldRec != null) {
                    return true;
                }
            }
            return false;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "update fileInfo failed:fileId=" + fileId + ",majorVersion=" + majorVersion
                            + ",minorVersion=" + minorVersion + ", externalData=" + externalData,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "update fileInfo failed:fileId=" + fileId + ",majorVersion=" + majorVersion
                            + ",minorVersion=" + minorVersion + ", externalData=" + externalData,
                    e);
        }
    }

    public void updateFileExternalData(ScmWorkspaceInfo wsInfo, BSONObject matcher,
            BSONObject externalData) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            fileAccessor.updateFileExternalData(matcher, externalData);

            MetaFileHistoryAccessor historyFileAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
            historyFileAccessor.updateFileExternalData(matcher, externalData);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "update file external data failed:matcher=" + matcher + ", extrenalData="
                            + externalData,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("update file external data failed:matcher=" + matcher
                    + ", extrenalData=" + externalData, e);
        }
    }

    public void updateFileInfo(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, BSONObject fileUpdator, BSONObject fileMatcher)
            throws ScmServerException {
        TransactionContext context = null;
        try {
            context = createTransactionContext();
            beginTransaction(context);
            boolean isUpdateSuccess = updateFileInfo(wsInfo, fileId, majorVersion, minorVersion,
                    fileUpdator, fileMatcher, context);
            if (isUpdateSuccess) {
                commitTransaction(context);
            }
            else {
                rollbackTransaction(context);
            }
        }
        catch (ScmServerException e) {
            rollbackTransaction(context);
            throw e;
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw new ScmSystemException("update fileInfo failed:fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion, e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    private boolean updateFileInfo(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, BSONObject fileUpdator, BSONObject fileMatcher,
            TransactionContext context) throws ScmServerException {
        try {
            BSONObject relUpdator = ScmMetaSourceHelper.createRelUpdatorByFileUpdator(fileUpdator);
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), context);

            BSONObject oldFileRecord = null;
            if (fileMatcher != null) {
                oldFileRecord = fileAccessor.updateFileInfo(fileId, majorVersion, minorVersion,
                        fileUpdator, fileMatcher);
            }
            else {
                oldFileRecord = fileAccessor.updateFileInfo(fileId, majorVersion, minorVersion,
                        fileUpdator);
            }

            if (oldFileRecord == null) {
                return false;
            }
            ScmFileOperateUtils.updateFileRelForUpdateFile(wsInfo, fileId, oldFileRecord,
                    relUpdator, context);
            return true;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "update fileInfo failed:fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("update fileInfo failed:fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion, e);
        }
    }

    /*
     * public boolean updateFileProperties(MetaSourceLocation location, String
     * wsName, String fileId, BSONObject classProperties) throws ScmServerException{
     * try { MetaFileAccessor mateFileAccessor =
     * metasource.getFileAccessor(location, wsName, null); return
     * mateFileAccessor.updateFileProperties(fileId, classProperties); } catch
     * (ScmServerException e) { throw new ScmServerException(e.getError(),
     * "updateBatchInfo failed: workspace=" + wsName, e); } catch (Exception e) {
     * throw new ScmSystemException( "updateBatchInfo failed: workspace=" + wsName ,
     * e); }
     *
     * }
     */

    public boolean updateTransId(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int status, String transId) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            return fileAccessor.updateTransId(fileId, majorVersion, minorVersion, status, transId);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "unmark transId from file failed:fileId="
                    + fileId + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("unmark transId from file failed:fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion, e);
        }
    }

    public void unmarkTransIdFromFile(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, int status) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            fileAccessor.unmarkTransId(fileId, majorVersion, minorVersion, status);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "unmark transId from file failed:fileId="
                    + fileId + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("unmark transId from file failed:fileId=" + fileId
                    + ",majorVersion=" + majorVersion + ",minorVersion=" + minorVersion, e);
        }
    }

    public BSONObject getFileInfo(ScmWorkspaceInfo wsInfo, String parentDirId, String fileName,
            int majorVersion, int minorVersion) throws ScmServerException {
        if (!wsInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to get file, directory is disable:ws=" + wsInfo.getName()
                            + ", parentDirId=" + parentDirId + ", fileName=" + fileName);
        }
        try {
            BSONObject relClMatcher = new BasicBSONObject();
            relClMatcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, parentDirId);
            relClMatcher.put(FieldName.FIELD_CLREL_FILENAME, fileName);
            MetaRelAccessor fileAccessor = getMetaSource().getRelAccessor(wsInfo.getName(), null);
            BSONObject relRec = ScmMetaSourceHelper.queryOne(fileAccessor, relClMatcher);
            if (relRec == null) {
                return null;
            }
            String fileId = (String) relRec.get(FieldName.FIELD_CLREL_FILEID);
            return getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, majorVersion,
                    minorVersion);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("getFileInfo failed:siteId=" + siteId + ",parentId="
                    + parentDirId + ",fileName=" + fileName + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion), e);
        }
    }

    public BSONObject getFileInfo(MetaSourceLocation location, String wsName, String fileID,
            int majorVersion, int minorVersion) throws ScmServerException {
        try {
            BSONObject currentFileMatcher = new BasicBSONObject();
            ScmMetaSourceHelper.addFileIdAndCreateMonth(currentFileMatcher, fileID);
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(location, wsName, null);
            BSONObject currentFile = ScmMetaSourceHelper.queryOne(fileAccessor, currentFileMatcher);
            if (currentFile == null) {
                logger.debug(
                        "get fileInfo failed, current file not exist:wsName={},fileId={},version={}.{}",
                        wsName, fileID, majorVersion, minorVersion);
                return null;
            }

            if (majorVersion == -1 && minorVersion == -1) {
                // not valid version, return current file info
                return currentFile;
            }

            int currentMajorVersion = (int) currentFile.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
            int currentMinorVersion = (int) currentFile.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
            if (currentMajorVersion == majorVersion && currentMinorVersion == minorVersion) {
                return currentFile;
            }

            BSONObject historyFileMatcher = new BasicBSONObject();
            historyFileMatcher.put(FieldName.FIELD_CLFILE_ID, fileID);
            historyFileMatcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH,
                    currentFile.get(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH));
            historyFileMatcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
            historyFileMatcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
            MetaFileHistoryAccessor historyAccessor = metasource.getFileHistoryAccessor(location,
                    wsName, null);
            BSONObject historyFile = ScmMetaSourceHelper.queryOne(historyAccessor,
                    historyFileMatcher);
            if (historyFile == null) {
                logger.debug(
                        "get fileInfo failed, history file not exist:wsName={},fileId={},version={}.{}",
                        wsName, fileID, majorVersion, minorVersion);
                return null;
            }

            return CommonHelper.completeHisotryFileRec(historyFile, currentFile);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "getFileInfo failed:siteId=" + siteId + ",fileID=" + fileID + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion),
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("getFileInfo failed:siteId=" + siteId + ",fileID=" + fileID
                    + ",version=" + ScmSystemUtils.getVersionStr(majorVersion, minorVersion), e);
        }
    }

    public String getFileId(ScmWorkspaceInfo ws, String parentDirId, String fileName)
            throws ScmServerException {
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to get file by file name and parent dir id, directory feature is disable:ws="
                            + ws.getName() + ", parentDirId=" + parentDirId + ", fileName="
                            + fileName);
        }
        try {
            BSONObject relClMatcher = new BasicBSONObject();
            relClMatcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, parentDirId);
            relClMatcher.put(FieldName.FIELD_CLREL_FILENAME, fileName);
            MetaRelAccessor fileAccessor = getMetaSource().getRelAccessor(ws.getName(), null);
            BSONObject relRec = ScmMetaSourceHelper.queryOne(fileAccessor, relClMatcher);
            if (relRec == null) {
                return null;
            }
            return (String) relRec.get(FieldName.FIELD_CLREL_FILEID);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("getFileId failed:siteId=" + siteId + ",parentId="
                    + parentDirId + ",fileName=" + fileName, e);
        }
    }

    public BSONObject getCurrentFileInfo(MetaSourceLocation location, String wsName, String fileID)
            throws ScmServerException {
        return getFileInfo(location, wsName, fileID, -1, -1);
    }

    public void deleteFile(ScmWorkspaceInfo ws, String fileID) throws ScmServerException {
        TransactionContext context = null;
        try {
            context = createTransactionContext();
            beginTransaction(context);
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(ws.getMetaLocation(),
                    ws.getName(), context);
            MetaFileHistoryAccessor historyAccessor = metasource
                    .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), context);
            BSONObject deletedFileRecord = fileAccessor.delete(fileID, -1, -1);
            if (deletedFileRecord != null) {
                ScmFileOperateUtils.deleteFileRelForDeleteFile(ws, fileID, deletedFileRecord,
                        context);
                historyAccessor.delete(fileID);
            }
            commitTransaction(context);
        }
        catch (ScmMetasourceException e) {
            logger.error("delete file failed:siteId=" + siteId + ",fileID=" + fileID);
            rollbackTransaction(context);
            throw new ScmServerException(e.getScmError(),
                    "delete file failed:siteId=" + siteId + ",fileID=" + fileID, e);
        }
        catch (Exception e) {
            logger.error("delete file failed:siteId=" + siteId + ",fileID=" + fileID);
            rollbackTransaction(context);
            throw e;
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public void insertTransLog(String wsName, BSONObject transRecord) throws ScmServerException {
        try {
            MetaTransLogAccessor transAccessor = metasource.getTransLogAccessor(wsName);
            transAccessor.insert(transRecord);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "insert trans log failed:siteId=" + siteId
                    + ",id=" + transRecord.get(FieldName.FIELD_CLTRANS_ID), e);
        }
        catch (Exception e) {
            throw new ScmSystemException("insert trans log failed:siteId=" + siteId + ",id="
                    + transRecord.get(FieldName.FIELD_CLTRANS_ID), e);
        }
    }

    public void deleteTransLog(String wsName, String transID) throws ScmServerException {
        try {
            MetaTransLogAccessor transAccessor = metasource.getTransLogAccessor(wsName);
            transAccessor.delete(transID);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "delete trans log failed:siteId=" + siteId + ",id=" + transID, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "delete trans log failed:siteId=" + siteId + ",id=" + transID, e);
        }
    }

    public BSONObject getTransLog(String wsName, String transID) throws ScmServerException {
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.FIELD_CLTRANS_ID, transID);
            MetaTransLogAccessor transAccessor = metasource.getTransLogAccessor(wsName);

            return ScmMetaSourceHelper.queryOne(transAccessor, matcher);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "getTransLog failed:siteId=" + siteId + ",transID=" + transID, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "getTransLog failed:siteId=" + siteId + ",transID=" + transID, e);
        }
    }

    public long getHistoryFileCount(MetaSourceLocation location, String wsName, BSONObject matcher)
            throws ScmServerException {
        try {
            return metasource.getFileHistoryAccessor(location, wsName, null).count(matcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get history file count: wsName=" + wsName + ", matcher=" + matcher,
                    e);
        }
    }

    public long getHistoryFileSizeSum(MetaSourceLocation location, String wsName,
            BSONObject matcher) throws ScmServerException {
        try {
            double sum = metasource.getFileHistoryAccessor(location, wsName, null).sum(matcher,
                    FieldName.FIELD_CLFILE_FILE_SIZE);
            return Math.round(sum);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get history file size sum: wsName=" + wsName + ", matcher="
                            + matcher,
                    e);
        }
    }

    public long getAllFileCount(MetaSourceLocation location, String wsName, BSONObject matcher)
            throws ScmServerException {
        try {
            long historyFileCount = metasource.getFileHistoryAccessor(location, wsName, null)
                    .count(matcher);
            long currentFileCount = metasource.getFileAccessor(location, wsName, null)
                    .count(matcher);
            return historyFileCount + currentFileCount;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get all file count: wsName=" + wsName + ", matcher=" + matcher, e);
        }
    }

    public long getAllFileSizeSum(MetaSourceLocation location, String wsName, BSONObject matcher)
            throws ScmServerException {
        try {
            double historyFileSum = metasource.getFileHistoryAccessor(location, wsName, null)
                    .sum(matcher, FieldName.FIELD_CLFILE_FILE_SIZE);
            double currentFileSum = metasource.getFileAccessor(location, wsName, null).sum(matcher,
                    FieldName.FIELD_CLFILE_FILE_SIZE);
            return Math.round(historyFileSum + currentFileSum);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get all file size sum: wsName=" + wsName + ", matcher=" + matcher,
                    e);
        }
    }

    public long getCurrentFileCount(ScmWorkspaceInfo ws, BSONObject matcher)
            throws ScmServerException {
        try {
            if (ScmMetaSourceHelper.parseFileMatcher(ws,
                    matcher) == ScmMetaSourceHelper.QUERY_IN_RELATION_TABLE) {
                logger.debug("count file in relation table:fileMatcher={}", matcher);
                BSONObject relMatcher = ScmMetaSourceHelper.getRelBSONFromFileBSON(matcher);
                logger.debug("change file matcher to relMatcher:relMatcher={}", relMatcher);
                return metasource.getRelAccessor(ws.getName(), null).count(relMatcher);
            }
            else {
                logger.debug("count file in file table,fileMatcher={}", matcher);
                return metasource.getFileAccessor(ws.getMetaLocation(), ws.getName(), null)
                        .count(matcher);
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "count file failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "count file failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
    }

    public long getCurrentFileSizeSum(ScmWorkspaceInfo ws, BSONObject matcher)
            throws ScmServerException {
        try {
            if (ScmMetaSourceHelper.parseFileMatcher(ws,
                    matcher) == ScmMetaSourceHelper.QUERY_IN_RELATION_TABLE) {
                logger.debug("sum file size in relation table:fileMatcher={}", matcher);
                BSONObject relMatcher = ScmMetaSourceHelper.getRelBSONFromFileBSON(matcher);
                logger.debug("change file matcher to relMatcher:relMatcher={}", relMatcher);
                double sum = metasource.getRelAccessor(ws.getName(), null).sum(relMatcher,
                        FieldName.FIELD_CLREL_FILE_SIZE);
                return Math.round(sum);
            }
            else {
                logger.debug("sum file size in file table,fileMatcher={}", matcher);
                double sum = metasource.getFileAccessor(ws.getMetaLocation(), ws.getName(), null)
                        .sum(matcher, FieldName.FIELD_CLFILE_FILE_SIZE);
                return Math.round(sum);
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "sum file size failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "sum file size failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
    }

    public void insertTask(BSONObject task) throws ScmServerException {
        try {
            MetaAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.insert(task);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "insertTask failed:siteId=" + siteId + ",task=" + task.toString(), e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "insertTask failed:siteId=" + siteId + ",task=" + task.toString(), e);
        }
    }

    public void deleteTask(String taskId) throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.delete(taskId);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "deleteTask failed:siteId=" + siteId + ",taskId=" + taskId, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("deleteTask failed:siteId=" + siteId + ",taskId=" + taskId,
                    e);
        }
    }

    public long countTask(BSONObject matcher) throws ScmServerException {
        try {
            MetaAccessor taskAccessor = metasource.getTaskAccessor();
            return taskAccessor.count(matcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "countTask failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "countTask failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
    }

    public void abortAllTask(int serverId, Date stopTime) throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.abortAllTask(serverId, stopTime);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "abortAllTask failed:siteId=" + siteId
                    + ",serverId=" + serverId + ",stopTime=" + stopTime, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("abortAllTask failed:siteId=" + siteId + ",serverId="
                    + serverId + ",stopTime=" + stopTime, e);
        }
    }

    public void setAllStopTime(int serverId, Date stopTime) throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.setAllStopTime(serverId, stopTime);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "setAllStopTime failed:siteId=" + siteId
                    + ",serverId=" + serverId + ",stopTime=" + stopTime, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("setAllStopTime failed:siteId=" + siteId + ",serverId="
                    + serverId + ",stopTime=" + stopTime, e);
        }
    }

    public void abortTask(String taskId, int flag, String detail, Date stopTime, int progress,
            long successCount, long failedCount) throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.abort(taskId, flag, detail, stopTime, progress, successCount, failedCount);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "abortTask failed:siteId=" + siteId + ",taskId=" + taskId + ",detail=" + detail,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "abortTask failed:siteId=" + siteId + ",taskId=" + taskId + ",detail=" + detail,
                    e);
        }
    }

    public void cancelTask(String taskId, String detail) throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.cancel(taskId, detail);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "cancelTask failed:siteId=" + siteId
                    + ",taskId=" + taskId + ",detail=" + detail, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("cancelTask failed:siteId=" + siteId + ",taskId=" + taskId
                    + ",detail=" + detail, e);
        }
    }

    public void updateTaskStopTimeIfEmpty(String taskId, Date stopTime, int progress,
            long successCount, long failedCount) throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.updateStopTimeIfEmpty(taskId, stopTime, progress, successCount,
                    failedCount);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "updateTaskStopTimeIfEmpty failed:siteId="
                    + siteId + ",taskId=" + taskId + ",stopTime=" + stopTime, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("updateTaskStopTimeIfEmpty failed:siteId=" + siteId
                    + ",taskId=" + taskId + ",stopTime=" + stopTime, e);
        }
    }

    public void finishTask(String taskId, Date stopTime, long successCount, long failedCount)
            throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.finish(taskId, stopTime, successCount, failedCount);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "finishTask failed:siteId=" + siteId
                    + ",taskId=" + taskId + ",stopTime=" + stopTime, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("finishTask failed:siteId=" + siteId + ",taskId=" + taskId
                    + ",stopTime=" + stopTime, e);
        }
    }

    public void startTask(String taskId, Date startTime, long estimateCount, long actualCount)
            throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.start(taskId, startTime, estimateCount, actualCount);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "startTask failed:siteId=" + siteId
                    + ",taskId=" + taskId + ",startTime=" + startTime, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("startTask failed:siteId=" + siteId + ",taskId=" + taskId
                    + ",startTime=" + startTime, e);
        }
    }

    public void updateTaskProgress(String taskId, int progress, long successCount, long failedCount)
            throws ScmServerException {
        try {
            MetaTaskAccessor taskAccessor = metasource.getTaskAccessor();
            taskAccessor.updateProgress(taskId, progress, successCount, failedCount);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "updateTaskProgress failed:siteId="
                    + siteId + ",taskId=" + taskId + ",progress=" + progress, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("updateTaskProgress failed:siteId=" + siteId + ",taskId="
                    + taskId + ",progress=" + progress, e);
        }
    }

    public BSONObject getTask(BSONObject matcher) throws ScmServerException {
        try {
            MetaAccessor taskAccessor = metasource.getTaskAccessor();
            return ScmMetaSourceHelper.queryOne(taskAccessor, matcher);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "getTaskInfo failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "getTaskInfo failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
    }

    public void insertDir(String wsName, BSONObject insertor) throws ScmServerException {
        try {
            MetaDirAccessor accessor = metasource.getDirAccessor(wsName);
            accessor.insert(insertor);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "insert directory failed:siteId=" + siteId + ",dir=" + insertor, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "insert directory failed:siteId=" + siteId + ",dir=" + insertor, e);
        }
    }

    public long updateDir(String wsName, String dirId, BSONObject updator)
            throws ScmServerException, ScmMetasourceException {
        TransactionContext context = null;
        try {
            context = createTransactionContext();
            beginTransaction(context);
            MetaDirAccessor dirAccessor = metasource.getDirAccessor(wsName, context);
            dirAccessor.updateDirInfo(dirId, updator);
            long newVersion = dirAccessor.updateVersion();
            commitTransaction(context);
            return newVersion;
        }
        catch (ScmMetasourceException e) {
            rollbackTransaction(context);
            throw e;
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw new ScmSystemException("updateDir failed:siteId=" + siteId + ", dirId=" + dirId
                    + ", updator=" + updator, e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public long deleteDir(String wsName, String dirId)
            throws ScmServerException, ScmMetasourceException {
        TransactionContext context = null;
        try {
            context = createTransactionContext();
            beginTransaction(context);
            MetaDirAccessor dirAccessor = metasource.getDirAccessor(wsName, context);
            dirAccessor.delete(dirId);
            long newVersion = dirAccessor.updateVersion();
            commitTransaction(context);
            return newVersion;
        }
        catch (ScmMetasourceException e) {
            rollbackTransaction(context);
            throw e;
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw new ScmSystemException("deleteDir failed:siteId=" + siteId + ", dirId=" + dirId,
                    e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public long getDirCount(String wsName, BSONObject matcher) throws ScmServerException {
        try {
            MetaDirAccessor accessor = metasource.getDirAccessor(wsName);
            return accessor.count(matcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "count directory failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "count directory failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
    }

    public BSONObject getRootDir(String wsName) throws ScmServerException {
        try {
            BSONObject rootMatcher = new BasicBSONObject();
            rootMatcher.put(FieldName.FIELD_CLDIR_ID, CommonDefine.Directory.SCM_ROOT_DIR_ID);
            BSONObject rootDir = ScmMetaSourceHelper.queryOne(metasource.getDirAccessor(wsName),
                    rootMatcher);
            if (rootDir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND, "root directory not exist");
            }
            return rootDir;
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(), "get root directory failed:siteId=" + siteId,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("get root directory failed:siteId=" + siteId, e);
        }
    }

    public BSONObject getDirByPath(String wsName, ScmDirPath path) throws ScmServerException {
        try {
            if (path.isRootDir()) {
                BSONObject rootDir = getRootDir(wsName);
                return rootDir;
            }

            MetaDirAccessor dirAccessor = metasource.getDirAccessor(wsName);

            BSONObject destDir = null;
            String parentId = CommonDefine.Directory.SCM_ROOT_DIR_ID;
            BSONObject matcher = new BasicBSONObject();
            int level = path.getLevel();
            for (int i = 2; i <= level; i++) {
                if (destDir != null) {
                    parentId = (String) destDir.get(FieldName.FIELD_CLDIR_ID);
                }
                String dirName = path.getNamebyLevel(i);
                matcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId);
                matcher.put(FieldName.FIELD_CLDIR_NAME, dirName);
                destDir = ScmMetaSourceHelper.queryOne(dirAccessor, matcher);
                if (destDir == null) {
                    return null;
                }
            }
            return destDir;
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "get directory failed:siteId=" + siteId + "path=" + path, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("get directory failed:siteId=" + siteId + "path=" + path,
                    e);
        }
    }

    public String getPathByDirId(String wsName, String dirId) throws ScmServerException {
        MetaDirAccessor dirAccessor = metasource.getDirAccessor(wsName);
        String path = "";
        while (!dirId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            BSONObject dirMatcher = new BasicBSONObject();
            dirMatcher.put(FieldName.FIELD_CLDIR_ID, dirId);
            BSONObject dir = ScmMetaSourceHelper.queryOne(dirAccessor, dirMatcher);
            if (dir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exists:dirId=" + dirId);
            }
            String name = (String) dir.get(FieldName.FIELD_CLDIR_NAME);
            path = name + "/" + path;
            dirId = (String) dir.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        }
        path = "/" + path;
        return path;

    }

    public BSONObject getDirInfo(String wsName, String dirId) throws ScmServerException {
        try {
            MetaDirAccessor accessor = metasource.getDirAccessor(wsName);
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.FIELD_CLDIR_ID, dirId);
            return ScmMetaSourceHelper.queryOne(accessor, matcher);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "count directory failed:siteId=" + siteId + ",directoryID=" + dirId, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "count directory failed:siteId=" + siteId + ",directoryID=" + dirId, e);
        }
    }

    public BSONObject getDirInfo(String wsName, String parentId, String dirName)
            throws ScmServerException {
        try {
            MetaDirAccessor accessor = metasource.getDirAccessor(wsName);
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId);
            matcher.put(FieldName.FIELD_CLDIR_NAME, dirName);
            return ScmMetaSourceHelper.queryOne(accessor, matcher);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(), "count directory failed:siteId=" + siteId
                    + ",parentDirId=" + parentId + ",dirName=" + dirName, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("count directory failed:siteId=" + siteId + ",parentDirId="
                    + parentId + ",dirName=" + dirName, e);
        }
    }

    public MetaCursor queryDirInfo(String wsName, BSONObject filter) throws ScmServerException {
        MetaDirAccessor dirAccessor = metasource.getDirAccessor(wsName);
        MetaCursor cursor = null;
        try {
            cursor = dirAccessor.query(filter, null, null, 0, 2, DBQuery.FLG_QUERY_WITH_RETURNDATA);
            return cursor;
        }
        catch (ScmMetasourceException e) {
            ScmSystemUtils.closeResource(cursor);
            throw new ScmServerException(e.getScmError(),
                    "queryDirInfo failed:workspace=" + wsName + ",filter=" + filter, e);
        }
    }

    public MetaCursor queryAllFile(ScmWorkspaceInfo ws, BSONObject matcher, BSONObject selector)
            throws ScmServerException {
        MetaCursor currentFileCursor = null;
        MetaCursor historyFileCursor = null;
        try {
            currentFileCursor = queryCurrentFile(ws, matcher, selector, null, 0, -1);
            historyFileCursor = queryHistoryFile(ws.getMetaLocation(), ws.getName(), matcher,
                    selector, null, 0, -1);
            return new AllFileMetaCursor(currentFileCursor, historyFileCursor);
        }
        catch (ScmMetasourceException e) {
            ScmSystemUtils.closeResource(currentFileCursor);
            ScmSystemUtils.closeResource(historyFileCursor);
            throw new ScmServerException(e.getScmError(),
                    String.format("Failed to query file in file table:fileMatcher=%s", matcher), e);
        }
        catch (Exception e) {
            ScmSystemUtils.closeResource(currentFileCursor);
            ScmSystemUtils.closeResource(historyFileCursor);
            throw new ScmSystemException(
                    String.format("Failed to query file in file table:fileMatcher=%s", matcher), e);
        }
    }

    public MetaCursor queryHistoryFile(MetaSourceLocation location, String wsName,
            BSONObject matcher, BSONObject selector, BSONObject orderby, long skip, long limit)
            throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(location, wsName, null);
            MetaFileHistoryAccessor historyAccessor = metasource.getFileHistoryAccessor(location,
                    wsName, null);
            return new HistoryFileMetaCursor(fileAccessor, historyAccessor, matcher, selector,
                    orderby, skip, limit);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    String.format(
                            "Failed to query file in history file table:fileMatcher=%s,selector=%s",
                            matcher, selector),
                    e);
        }
    }

    public MetaCursor queryCurrentFile(ScmWorkspaceInfo ws, BSONObject matcher, BSONObject selector,
            BSONObject orderby, long skip, long limit) throws ScmServerException {
        try {
            int matcherParseRes = ScmMetaSourceHelper.parseFileMatcher(ws, matcher);
            int selectorParseRes = ScmMetaSourceHelper.parseFileSelector(ws, selector);
            int orderbyParseRes = ScmMetaSourceHelper.parseFileOderby(ws, orderby);

            if (matcherParseRes == ScmMetaSourceHelper.QUERY_IN_RELATION_TABLE
                    && selectorParseRes == ScmMetaSourceHelper.QUERY_IN_RELATION_TABLE
                    && orderbyParseRes == ScmMetaSourceHelper.QUERY_IN_RELATION_TABLE) {
                logger.debug(
                        "query file in relation table:fileMatcher={},fileSelector={},fileOrderby={}",
                        matcher, selector, orderby);
                BSONObject relMatcher = ScmMetaSourceHelper.getRelBSONFromFileBSON(matcher);
                BSONObject relSelector = ScmMetaSourceHelper.getRelBSONFromFileBSON(selector);
                BSONObject relOrderby = ScmMetaSourceHelper.getRelBSONFromFileBSON(orderby);
                logger.debug(
                        "change file matcher to relMatcher:relMatcher={},relSelector={},relOrderby={}",
                        relMatcher, relSelector, relOrderby);
                return new RelMetaCursor(metasource.getRelAccessor(ws.getName(), null)
                        .query(relMatcher, relSelector, relOrderby, skip, limit));
            }
            else {
                logger.debug(
                        "query file in file table:fileMatcher={},fileSelector={},fileOrderby={}",
                        matcher, selector, orderby);
                return metasource.getFileAccessor(ws.getMetaLocation(), ws.getName(), null)
                        .query(matcher, selector, orderby, skip, limit);
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "query file failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "query file failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
    }

    public BreakpointFile getBreakpointFile(String workspaceName, String fileName)
            throws ScmServerException {
        MetaBreakpointFileAccessor accessor = metasource.getBreakpointFileAccessor(workspaceName,
                null);
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLBREAKPOINTFILE_FILE_NAME, fileName);
        BSONObject fileObj = ScmMetaSourceHelper.queryOne(accessor, matcher);
        if (fileObj == null) {
            return null;
        }

        return bsonobjToBreakpointFile(workspaceName, fileObj);
    }

    public List<BreakpointFile> listBreakpointFiles(String workspaceName, BSONObject filter)
            throws ScmServerException {
        MetaBreakpointFileAccessor accessor = metasource.getBreakpointFileAccessor(workspaceName,
                null);

        List<BreakpointFile> list = new ArrayList<>();
        try (MetaCursor cursor = accessor.query(filter, null, null)) {
            while (cursor.hasNext()) {
                BSONObject fileObj = cursor.getNext();
                list.add(bsonobjToBreakpointFile(workspaceName, fileObj));
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "workspace=" + workspaceName + "filter=" + filter, e);
        }
        return list;
    }

    private BreakpointFile bsonobjToBreakpointFile(String workspaceName, BSONObject fileObj) {
        BreakpointFile file = breakpointFileBsonConverter.convert(fileObj);
        file.setWorkspaceName(workspaceName);

        ScmSite site = ScmContentServer.getInstance().getSiteInfo(file.getSiteId());
        file.setSiteName(site.getName());
        return file;
    }

    public BreakpointFile createBreakpointFile(String createUser, String workspaceName,
            String fileName, ChecksumType checksumType, long createTime, boolean isNeedMd5)
            throws ScmServerException {
        MetaBreakpointFileAccessor accessor = metasource.getBreakpointFileAccessor(workspaceName,
                null);

        ScmSite site = ScmContentServer.getInstance().getLocalSiteInfo();

        BreakpointFile file = new BreakpointFile();
        file.setWorkspaceName(workspaceName).setFileName(fileName).setChecksumType(checksumType)
                .setSiteId(site.getId()).setSiteName(site.getName()).setCreateUser(createUser)
                .setCreateTime(createTime).setNeedMd5(isNeedMd5);

        try {
            accessor.insert(breakpointFileBsonConverter.convert(file));
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    String.format("Failed to create breakpoint file:/%s/%s", workspaceName, file),
                    e);
        }
        return file;
    }

    public void updateBreakpointFile(BreakpointFile file) throws ScmServerException {
        MetaBreakpointFileAccessor accessor = metasource
                .getBreakpointFileAccessor(file.getWorkspaceName(), null);

        BSONObject matcher = new BasicBSONObject(BreakpointFileBsonConverter.BSON_FIELD_FILE_NAME,
                file.getFileName());
        BSONObject updateObj = new BasicBSONObject();
        // only update below fields
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_DATA_ID, file.getDataId());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_COMPLETED, file.isCompleted());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_CHECKSUM, file.getChecksum());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_UPLOAD_SIZE, file.getUploadSize());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_UPLOAD_USER, file.getUploadUser());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_UPLOAD_TIME, file.getUploadTime());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_IS_NEED_MD5, file.isNeedMd5());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_MD5, file.getMd5());
        updateObj.put(BreakpointFileBsonConverter.BSON_FIELD_EXTRA_CONTEXT, file.getExtraContext());

        BSONObject updater = new BasicBSONObject("$set", updateObj);
        try {
            accessor.update(matcher, updater);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    String.format("Failed to update breakpoint file:/%s/%s",
                            file.getWorkspaceName(), file.getFileName()),
                    e);
        }
    }

    private void deleteBreakpointFile(String workspaceName, String fileName,
            TransactionContext context) throws ScmServerException {
        MetaBreakpointFileAccessor accessor = metasource.getBreakpointFileAccessor(workspaceName,
                context);
        try {
            accessor.delete(fileName);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), String
                    .format("Failed to delete breakpoint file:/%s/%s", workspaceName, fileName), e);
        }
    }

    public void deleteBreakpointFile(BreakpointFile file) throws ScmServerException {
        deleteBreakpointFile(file.getWorkspaceName(), file.getFileName(), null);
    }

    public void breakpointFileToFile(ScmWorkspaceInfo wsInfo, BreakpointFile breakpointFile,
            BSONObject fileInfo) throws ScmServerException {
        TransactionContext context = null;
        try {
            context = createTransactionContext();
            beginTransaction(context);
            insertFile(wsInfo, fileInfo, context);
            deleteBreakpointFile(wsInfo.getName(), breakpointFile.getFileName(), context);
            commitTransaction(context);
        }
        catch (ScmServerException e) {
            rollbackTransaction(context);
            if (e.getError() == ScmError.FILE_TABLE_NOT_FOUND) {
                logger.debug("create table", e);
                try {
                    metasource.getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), context)
                            .createFileTable(fileInfo);
                }
                catch (ScmMetasourceException ex) {
                    throw new ScmServerException(ex.getScmError(),
                            "Failed to create table: " + fileInfo, e);
                }
                breakpointFileToFile(wsInfo, breakpointFile, fileInfo);
                return;
            }
            throw new ScmServerException(e.getError(),
                    "Failed to insert file by breakpoint file:" + "siteId=" + siteId + ",file="
                            + fileInfo.get(FieldName.FIELD_CLFILE_ID) + ", breakpointFile="
                            + breakpointFile.getFileName(),
                    e);
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw new ScmSystemException("Failed to insert file by breakpoint file:" + "siteId="
                    + siteId + ",file=" + fileInfo.get(FieldName.FIELD_CLFILE_ID)
                    + ", breakpointFile=" + breakpointFile.getFileName(), e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    private TransactionContext createTransactionContext() throws ScmServerException {
        try {
            return metasource.createTransactionContext();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to create transaction context",
                    e);
        }
    }

    private void beginTransaction(TransactionContext context) throws ScmServerException {
        try {
            context.begin();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to begin transaction", e);
        }
    }

    private void commitTransaction(TransactionContext context) throws ScmServerException {
        try {
            context.commit();
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to commit transaction", e);
        }
    }

    private void rollbackTransaction(TransactionContext context) {
        if (context != null) {
            context.rollback();
        }
    }

    private void closeTransactionContext(TransactionContext context) {
        if (context != null) {
            context.close();
        }
    }

    /********* batch ***********/
    public void insertBatch(ScmWorkspaceInfo wsInfo, BSONObject batch) throws ScmServerException {
        MetaBatchAccessor batchAccessor = metasource.getBatchAccessor(wsInfo.getName(), null);
        try {
            batchAccessor.insert(batch);
        }
        catch (ScmMetasourceException e) {
            if (wsInfo.isBatchSharding() && e.getScmError() == ScmError.FILE_TABLE_NOT_FOUND) {
                logger.debug("create table", e);
                try {
                    Date createDate = new Date(BsonUtils
                            .getNumberChecked(batch, FieldName.Batch.FIELD_INNER_CREATE_TIME)
                            .longValue());
                    batchAccessor.createSubTable(wsInfo.getBatchShardingType(), createDate);
                }
                catch (Exception ex) {
                    throw new ScmServerException(ScmError.METASOURCE_ERROR,
                            "insert batch failed, create batch sub table failed:ws="
                                    + wsInfo.getName() + ",siteId=" + siteId + ",file="
                                    + batch.get(FieldName.Batch.FIELD_ID),
                            ex);
                }
                insertBatch(wsInfo, batch);
                return;
            }
            throw new ScmServerException(e.getScmError(), "insertBatch failed: workspace="
                    + wsInfo.getName() + ",batch=" + batch.toString(), e);
        }
        catch (Exception e) {
            throw new ScmSystemException("insertBatch failed: workspace=" + wsInfo.getName()
                    + ",batch=" + batch.toString(), e);
        }
    }

    public BSONObject getBatchInfo(ScmWorkspaceInfo ws, String batchId, String createMonth)
            throws ScmServerException {
        try {
            MetaBatchAccessor batchAccessor = metasource.getBatchAccessor(ws.getName(), null);
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.Batch.FIELD_ID, batchId);
            if (createMonth != null) {
                matcher.put(FieldName.Batch.FIELD_INNER_CREATE_MONTH, createMonth);
            }
            return ScmMetaSourceHelper.queryOne(batchAccessor, matcher);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "getBatchInfo failed:workspace=" + ws.getName() + ",batchId=" + batchId, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "getBatchInfo failed:workspace=" + ws.getName() + ",batchId=" + batchId, e);
        }
    }

    public void deleteBatch(ScmWorkspaceInfo ws, String batchId, String batchCreateMonth,
            String sessionId, String userDetail, String user, FileOperationListenerMgr listenerMgr)
            throws ScmServerException {
        try {
            // check whether the batch exists
            BSONObject batch = getBatchInfo(ws, batchId, batchCreateMonth);
            if (null == batch) {
                throw new ScmServerException(ScmError.BATCH_NOT_FOUND,
                        "batch not found:ws=" + ws.getName() + ",batchId=" + batchId);
            }

            MetaBatchAccessor batchAccessor = metasource.getBatchAccessor(ws.getName(), null);
            FileDeletorDao fileDeletorDao = new FileDeletorDao();
            BasicBSONList files = (BasicBSONList) batch.get(FieldName.Batch.FIELD_FILES);
            for (Object obj : files) {
                BSONObject file = (BSONObject) obj;
                String fileId = (String) file.get(FieldName.FIELD_CLFILE_ID);
                // detach file
                batchDetachFile(ws.getName(), batchCreateMonth, batchId, fileId, user);
                // delete file
                fileDeletorDao.init(sessionId, userDetail, ws, fileId, -1, -1, true, listenerMgr);
                fileDeletorDao.delete();
            }
            batchAccessor.delete(batchId, batchCreateMonth);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "deleteBatch failed: workspace=" + ws.getName() + ",batchId=" + batchId, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "deleteBatch failed: workspace=" + ws.getName() + ",batchId=" + batchId, e);
        }
    }

    public MetaCursor getBatchList(String wsName, BSONObject matcher, BSONObject selector,
            BSONObject orderBy, long skip, long limit) throws ScmServerException {
        try {
            MetaBatchAccessor batchAccessor = metasource.getBatchAccessor(wsName, null);
            MetaCursor cursor = batchAccessor.query(matcher, selector, orderBy, skip, limit);
            return cursor;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "getBatchList failed: workspace=" + wsName + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "getBatchList failed: workspace=" + wsName + ",matcher=" + matcher, e);
        }
    }

    public void batchAttachFile(ScmWorkspaceInfo ws, String batchId, String batchCreateMonth,
            String fileId, String updateUser) throws ScmServerException {
        TransactionContext context = null;
        try {
            context = metasource.createTransactionContext();
            MetaBatchAccessor batchAccessor = metasource.getBatchAccessor(ws.getName(), context);

            context.begin();
            updateBatchIdOfFile(ws.getName(), batchId, fileId, updateUser, context);
            batchAccessor.attachFile(batchId, batchCreateMonth, fileId, updateUser);
            context.commit();
        }
        catch (ScmServerException e) {
            rollbackTransaction(context);
            throw new ScmServerException(e.getError(), "batchAttachFile failed: workspace="
                    + ws.getName() + ",batchId=" + batchId + ",fileId=" + fileId, e);
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw new ScmSystemException("batchAttachFile failed: workspace=" + ws.getName()
                    + ",batchId=" + batchId + ",fileId=" + fileId, e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public void batchDetachFile(String wsName, String batchId, String batchCreateMonth,
            String fileId, String updateUser) throws ScmServerException {
        TransactionContext context = null;
        try {
            context = metasource.createTransactionContext();
            MetaBatchAccessor batchAccessor = metasource.getBatchAccessor(wsName, context);

            context.begin();
            batchAccessor.detachFile(batchId, batchCreateMonth, fileId, updateUser);
            updateBatchIdOfFile(wsName, "", fileId, updateUser, context);
            context.commit();
        }
        catch (ScmServerException e) {
            rollbackTransaction(context);
            throw new ScmServerException(e.getError(), "batchDetachFile failed: workspace=" + wsName
                    + ",batchId=" + batchId + ",fileId=" + fileId, e);
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw new ScmSystemException("batchDetachFile failed: workspace=" + wsName + ",batchId="
                    + batchId + ",fileId=" + fileId, e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public void updateBatchIdOfFile(String wsName, String newBatchId, String fileId, String user,
            TransactionContext context) throws ScmServerException {
        BSONObject updator = new BasicBSONObject(FieldName.FIELD_CLFILE_BATCH_ID, newBatchId);
        updator.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, user);
        Date updateTime = new Date();
        updator.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, updateTime.getTime());
        // BasicBSONObject fileMatcher = new
        // BasicBSONObject(FieldName.FIELD_CLFILE_BATCH_ID, oldBatchId);
        if (null == context) {
            updateFileInfo(ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName), fileId,
                    -1, -1, updator);
        }
        else {
            updateFileInfo(ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName), fileId,
                    -1, -1, updator, null, context);
        }
    }

    public boolean updateBatchInfo(String wsName, String batchId, String batchCreateMonth,
            BSONObject updator) throws ScmServerException {
        try {
            MetaBatchAccessor batchAccessor = metasource.getBatchAccessor(wsName, null);
            return batchAccessor.update(batchId, batchCreateMonth, updator);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "updateBatchInfo failed: workspace="
                    + wsName + ",batchId=" + batchId + ",updator=" + updator, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("updateBatchInfo failed: workspace=" + wsName + ",batchId="
                    + batchId + ",updator=" + updator, e);
        }
    }

    public ContentModuleMetaSource getMetaSource() {
        return metasource;
    }

    public void close() {
        metasource.close();
        metasource = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("@").append(Integer.toHexString(hashCode()))
                .append(",");
        sb.append("siteId=").append(siteId).append(",");
        sb.append(metasource.toString());
        return sb.toString();
    }

    public void addNewFileVersion(ScmWorkspaceInfo ws, String fileId, BSONObject historyRec,
            BSONObject currentRecUpdator) throws ScmServerException {
        TransactionContext transaction = createTransactionContext();
        try {
            beginTransaction(transaction);
            insertHistoryFileAndUpdateCurrentFile(ws, fileId, historyRec, currentRecUpdator,
                    transaction);
            commitTransaction(transaction);
        }
        catch (ScmMetasourceException e) {
            rollbackTransaction(transaction);
            throw new ScmServerException(e.getScmError(), "Failed to add new file version, fileId="
                    + fileId + ", historyRec=" + historyRec + ", updater=" + currentRecUpdator, e);
        }
        catch (Exception e) {
            rollbackTransaction(transaction);
            throw e;
        }
        finally {
            closeTransactionContext(transaction);
        }
    }

    private void insertHistoryFileAndUpdateCurrentFile(ScmWorkspaceInfo ws, String fileId,
            BSONObject historyRec, BSONObject currentRecUpdator, TransactionContext transaction)
            throws ScmMetasourceException, ScmServerException {
        MetaFileAccessor fileAccessor = metasource.getFileAccessor(ws.getMetaLocation(),
                ws.getName(), transaction);
        MetaFileHistoryAccessor historyAccessor = metasource
                .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), transaction);
        historyAccessor.insert(historyRec);
        BSONObject oldFileRecord = fileAccessor.updateFileInfo(fileId, -1, -1, currentRecUpdator);
        if (oldFileRecord == null) {
            throw new ScmSystemException(
                    "add new file version failed, file not exist, we should lock it first:ws="
                            + ws.getName() + ",fileId=" + fileId);
        }
        BSONObject relUpdator = ScmMetaSourceHelper
                .createRelUpdatorByFileUpdator(currentRecUpdator);
        ScmFileOperateUtils.updateFileRelForUpdateFile(ws, fileId, oldFileRecord, relUpdator,
                transaction);
    }

    public void breakpointFileToNewVersionFile(ScmWorkspaceInfo ws, String breakpointFile,
            String fileId, BSONObject historyRec, BSONObject currentRecUpdator)
            throws ScmServerException {
        TransactionContext transaction = createTransactionContext();
        try {
            beginTransaction(transaction);
            MetaBreakpointFileAccessor breakpointAccessor = metasource
                    .getBreakpointFileAccessor(ws.getName(), transaction);
            insertHistoryFileAndUpdateCurrentFile(ws, fileId, historyRec, currentRecUpdator,
                    transaction);
            breakpointAccessor.delete(breakpointFile);
            commitTransaction(transaction);
        }
        catch (ScmMetasourceException e) {
            rollbackTransaction(transaction);
            throw new ScmServerException(e.getScmError(),
                    "Failed to change breakpoint to new version file, fileId=" + fileId
                            + ", historyRec=" + historyRec + ", updater=" + currentRecUpdator,
                    e);
        }
        catch (Exception e) {
            rollbackTransaction(transaction);
            throw e;
        }
        finally {
            closeTransactionContext(transaction);
        }
    }

    public void insertClass(String workspaceName, BSONObject classInfo) throws ScmServerException {
        try {
            MetaClassAccessor classAccessor = metasource.getClassAccessor(workspaceName, null);
            classAccessor.insert(classInfo);
        }
        catch (ScmMetasourceException e) {
            // class name is unique
            if (e.getScmError() == ScmError.METADATA_CLASS_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_EXIST,
                        "a class with the same name already exists:name="
                                + classInfo.get(FieldName.Class.FIELD_NAME),
                        e);
            }
            throw new ScmServerException(e.getScmError(), "insertClass failed: workspace="
                    + workspaceName + ",classInfo=" + classInfo.toString(), e);
        }
        catch (Exception e) {
            throw new ScmSystemException("insertClass failed: workspace=" + workspaceName
                    + ",classInfo=" + classInfo.toString(), e);
        }
    }

    public List<MetadataClass> listClassInfo(String wsName, BSONObject filter)
            throws ScmServerException {
        MetaClassAccessor classAccessor = metasource.getClassAccessor(wsName, null);
        try (MetaCursor cursor = classAccessor.query(filter, null, null)) {
            List<MetadataClass> list = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject classObj = cursor.getNext();
                list.add(metadataClassBsonConverter.convert(classObj));
            }
            return list;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "getClassList failed:workspace=" + wsName + ",filter=" + filter, e);
        }
    }

    public MetadataClass getClassInfo(String wsName, BSONObject matcher) throws ScmServerException {
        try {
            MetaClassAccessor classAccessor = metasource.getClassAccessor(wsName, null);
            BSONObject classObj = ScmMetaSourceHelper.queryOne(classAccessor, matcher);
            if (classObj == null) {
                return null;
            }
            return metadataClassBsonConverter.convert(classObj);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "getClassInfo failed:workspace=" + wsName + ",matcher=" + matcher, e);
        }
    }

    public boolean updateClassInfo(String wsName, String classId, BSONObject updator)
            throws ScmServerException {
        try {
            MetaClassAccessor classAccessor = metasource.getClassAccessor(wsName, null);
            return classAccessor.update(classId, updator);
        }
        catch (ScmMetasourceException e) {
            // class name is unique
            if (updator.containsField(FieldName.Class.FIELD_NAME)
                    && e.getScmError() == ScmError.METADATA_CLASS_EXIST) {
                throw new ScmServerException(ScmError.METADATA_CLASS_EXIST,
                        "a class with the same name already exists:name="
                                + updator.get(FieldName.Class.FIELD_NAME),
                        e);
            }
            throw new ScmServerException(e.getScmError(), "updateClassInfo failed: workspace="
                    + wsName + ",classId=" + classId + ",updator=" + updator, e);
        }
    }

    public void deleteClass(String wsName, String classId) throws ScmServerException {
        TransactionContext context = null;
        try {
            context = metasource.createTransactionContext();
            MetaClassAccessor classAccessor = metasource.getClassAccessor(wsName, context);
            MetaClassAttrRelAccessor classAttrRelAccessor = metasource
                    .getClassAttrRelAccessor(wsName, context);

            context.begin();
            classAttrRelAccessor.deleteByClassId(classId);
            classAccessor.delete(classId);
            context.commit();
        }
        catch (ScmMetasourceException e) {
            rollbackTransaction(context);
            throw new ScmServerException(e.getScmError(),
                    "deleteClass failed:workspace=" + wsName + ",classId=" + classId, e);
        }
        catch (Exception e) {
            rollbackTransaction(context);
            throw e;
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public List<MetadataAttr> getAttrListForClass(String wsName, String classId)
            throws ScmServerException {
        MetaCursor relCursor = null;
        try {
            ArrayList<MetadataAttr> attrList = new ArrayList<>();
            BSONObject matcher = new BasicBSONObject(FieldName.ClassAttrRel.FIELD_CLASS_ID,
                    classId);
            MetaClassAttrRelAccessor classAttrRelAccessor = metasource
                    .getClassAttrRelAccessor(wsName, null);
            relCursor = classAttrRelAccessor.query(matcher, null, null);
            if (null != relCursor) {
                while (relCursor.hasNext()) {
                    String attrId = (String) relCursor.getNext()
                            .get(FieldName.ClassAttrRel.FIELD_ATTR_ID);
                    attrList.add(getAttrInfo(wsName, attrId));
                }
            }
            return attrList;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "get class_attr_rel failed:class_id:" + classId, e);
        }
        finally {
            ScmSystemUtils.closeResource(relCursor);
        }
    }

    public List<String> getClassIdsForAttr(String wsName, String attrId) throws ScmServerException {
        MetaCursor relCursor = null;
        try {
            ArrayList<String> classIds = new ArrayList<>();
            BSONObject matcher = new BasicBSONObject(FieldName.ClassAttrRel.FIELD_ATTR_ID, attrId);
            MetaClassAttrRelAccessor classAttrRelAccessor = metasource
                    .getClassAttrRelAccessor(wsName, null);
            relCursor = classAttrRelAccessor.query(matcher, null, null);
            if (null != relCursor) {
                while (relCursor.hasNext()) {
                    String classId = (String) relCursor.getNext()
                            .get(FieldName.ClassAttrRel.FIELD_CLASS_ID);
                    classIds.add(classId);
                }
            }
            return classIds;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "getClassIds failed:attrId:" + attrId, e);
        }
        finally {
            ScmSystemUtils.closeResource(relCursor);
        }
    }

    public void classAttachAttr(String wsName, String classId, String attrId, String user)
            throws ScmServerException {
        TransactionContext context = null;
        try {
            context = metasource.createTransactionContext();
            MetaClassAccessor classAccessor = metasource.getClassAccessor(wsName, context);
            MetaClassAttrRelAccessor classAttrRelAccessor = metasource
                    .getClassAttrRelAccessor(wsName, context);

            context.begin();
            BasicBSONObject insertor = new BasicBSONObject();
            insertor.put(FieldName.ClassAttrRel.FIELD_CLASS_ID, classId);
            insertor.put(FieldName.ClassAttrRel.FIELD_ATTR_ID, attrId);
            classAttrRelAccessor.insert(insertor);

            BasicBSONObject updator = new BasicBSONObject();
            updator.put(FieldName.Class.FIELD_INNER_UPDATE_USER, user);
            Date updateTime = new Date();
            updator.put(FieldName.Class.FIELD_INNER_UPDATE_TIME, updateTime.getTime());
            classAccessor.update(classId, updator);
            context.commit();
        }
        catch (ScmMetasourceException e) {
            rollbackTransaction(context);
            // cannot attach repeat
            if (e.getScmError() == ScmError.METADATA_ATTR_ALREADY_IN_CLASS) {
                throw new ScmServerException(ScmError.METADATA_ATTR_ALREADY_IN_CLASS,
                        "the class is already attached with this attr:workspace=" + wsName
                                + ",classId=" + classId + ",attrId=" + attrId,
                        e);
            }
            throw new ScmServerException(e.getScmError(), "classAttachAttr failed:workspace="
                    + wsName + ",classId=" + classId + ",attrId=" + attrId, e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public void classDetachAttr(String wsName, String classId, String attrId, String user)
            throws ScmServerException {
        TransactionContext context = null;
        try {
            context = metasource.createTransactionContext();
            MetaClassAccessor classAccessor = metasource.getClassAccessor(wsName, context);
            MetaClassAttrRelAccessor classAttrRelAccessor = metasource
                    .getClassAttrRelAccessor(wsName, context);

            context.begin();
            classAttrRelAccessor.delete(classId, attrId);

            BasicBSONObject updator = new BasicBSONObject();
            updator.put(FieldName.Class.FIELD_INNER_UPDATE_USER, user);
            Date updateTime = new Date();
            updator.put(FieldName.Class.FIELD_INNER_UPDATE_TIME, updateTime.getTime());
            classAccessor.update(classId, updator);
            context.commit();
        }
        catch (ScmMetasourceException e) {
            rollbackTransaction(context);
            throw new ScmServerException(e.getScmError(), "classDetachAttr failed:workspace="
                    + wsName + ",classId=" + classId + ",attrId=" + attrId, e);
        }
        finally {
            closeTransactionContext(context);
        }
    }

    public void insertAttr(String workspaceName, BSONObject attrInfo) throws ScmServerException {
        try {
            MetaAttrAccessor attrAccessor = metasource.getAttrAccessor(workspaceName);
            attrAccessor.insert(attrInfo);
        }
        catch (ScmMetasourceException e) {
            // class name is unique
            if (e.getScmError() == ScmError.METADATA_ATTR_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_EXIST,
                        "a attr with the same name already exists:name="
                                + attrInfo.get(FieldName.Attribute.FIELD_NAME),
                        e);
            }
            throw new ScmServerException(e.getScmError(), "insertAttr failed: workspace="
                    + workspaceName + ",attrInfo=" + attrInfo.toString(), e);
        }
        catch (Exception e) {
            throw new ScmSystemException("insertAttr failed: workspace=" + workspaceName
                    + ",attrInfo=" + attrInfo.toString(), e);
        }
    }

    public List<MetadataAttr> listAttrInfo(String wsName, BSONObject filter)
            throws ScmServerException {
        MetaAttrAccessor attrAccessor = metasource.getAttrAccessor(wsName);
        try (MetaCursor cursor = attrAccessor.query(filter, null, null)) {
            List<MetadataAttr> list = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject attrObj = cursor.getNext();
                list.add(metadataAttrBsonConverter.convert(attrObj));
            }
            return list;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "getAttrList failed:workspace=" + wsName + ",filter=" + filter, e);
        }
    }

    public MetadataAttr getAttrInfo(String wsName, String attrId) throws ScmServerException {
        try {
            MetaAttrAccessor attrAccessor = metasource.getAttrAccessor(wsName);
            BSONObject matcher = new BasicBSONObject(FieldName.Attribute.FIELD_ID, attrId);
            BSONObject attrObj = ScmMetaSourceHelper.queryOne(attrAccessor, matcher);
            if (attrObj == null) {
                return null;
            }

            return metadataAttrBsonConverter.convert(attrObj);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "getAttrInfo failed:workspace=" + wsName + ",attrId=" + attrId, e);
        }
    }

    public boolean updateAttrInfo(String wsName, String attrId, BSONObject updator)
            throws ScmServerException {
        try {
            MetaAttrAccessor attrAccessor = metasource.getAttrAccessor(wsName);
            return attrAccessor.update(attrId, updator);
        }
        catch (ScmMetasourceException e) {
            // attr name is unique
            if (updator.containsField(FieldName.Attribute.FIELD_NAME)
                    && e.getScmError() == ScmError.METADATA_ATTR_EXIST) {
                throw new ScmServerException(ScmError.METADATA_ATTR_EXIST,
                        "a attr with the same name already exists:name="
                                + updator.get(FieldName.Attribute.FIELD_NAME),
                        e);
            }
            throw new ScmServerException(e.getScmError(), "updateAttrInfo failed: workspace="
                    + wsName + ",attrId=" + attrId + ",updator=" + updator, e);
        }
    }

    public void deleteAttr(String wsName, String attrId) throws ScmServerException {
        try {
            MetaAttrAccessor attrAccessor = metasource.getAttrAccessor(wsName);
            attrAccessor.delete(attrId);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "deleteAttr failed:workspace=" + wsName + ",attrId=" + attrId, e);
        }
    }

    public List<BSONObject> getAllStrategyInfo() throws ScmServerException {
        MetaCursor cursor = null;
        List<BSONObject> strategyList = new ArrayList<>();
        try {
            MetaAccessor strategyAccessor = metasource.getStrategyAccessor();
            cursor = strategyAccessor.query(null, null, null);
            while (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                strategyList.add(obj);
            }
            return strategyList;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Get all strategy info failed", e);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "Get all strategy info failed", e);
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public MetaCursor getAuditList(BSONObject matcher) throws ScmServerException {
        try {
            MetaAccessor AuditAccessor = metasource.getAuditAccessor();
            MetaCursor cursor = AuditAccessor.query(matcher, null, null);

            return cursor;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "getBatchList failed: matcher=" + matcher,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("getBatchList failed: matcher=" + matcher, e);
        }
    }

    public void recordDataTableName(String wsName, String tableName) throws ScmMetasourceException {
        BasicBSONObject rec = new BasicBSONObject();
        rec.put(FieldName.DataTableNameHistory.WORKSPACE_NAME, wsName);
        rec.put(FieldName.DataTableNameHistory.SITE_NAME,
                ScmContentServer.getInstance().getLocalSiteInfo().getName());
        rec.put(FieldName.DataTableNameHistory.TABLE_CREATE_TIME, System.currentTimeMillis());
        rec.put(FieldName.DataTableNameHistory.TABLE_NAME, tableName);
        rec.put(FieldName.DataTableNameHistory.WORKSPACE_IS_DELETED, false);
        metasource.getDataTableNameHistoryAccessor().insert(rec);
    }

    public long getBatchCount(String wsName, BSONObject matcher) throws ScmServerException {
        try {
            MetaBatchAccessor accessor = metasource.getBatchAccessor(wsName, null);
            return accessor.count(matcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "count batch failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "count batch failed:siteId=" + siteId + ",matcher=" + matcher, e);
        }
    }

    public void updateFileMd5(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, String md5) throws ScmServerException {
        try {
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);
            BSONObject ret = fileAccessor.updateFileInfo(fileId, majorVersion, minorVersion,
                    new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_MD5, md5));
            if (ret != null) {
                return;
            }

            MetaFileHistoryAccessor historyAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null);
            historyAccessor.updateMd5(fileId, majorVersion, minorVersion, md5);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "update md5 failed:fileId=" + fileId + ",majorVersion=" + majorVersion
                            + ",minorVersion=" + minorVersion + ", md5=" + md5,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException("update md5 failed:fileId=" + fileId + ",majorVersion="
                    + majorVersion + ",minorVersion=" + minorVersion + ",md5=" + md5, e);
        }
    }
}