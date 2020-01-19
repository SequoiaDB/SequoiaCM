package com.sequoiacm.contentserver.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class FileContentUpdateDao {
    private static final Logger logger = LoggerFactory.getLogger(FileContentUpdateDao.class);

    private String user;
    private String fileId;
    private int clientMajorVersion;
    private int clientMinorVersion;
    private ScmWorkspaceInfo ws;
    private ScmContentServer contentserver = ScmContentServer.getInstance();

    public FileContentUpdateDao(String user, String wsName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        this.user = user;
        this.fileId = fileId;
        this.clientMajorVersion = majorVersion;
        this.clientMinorVersion = minorVersion;
        this.ws = contentserver.getWorkspaceInfoChecked(wsName);
    }

    public BSONObject updateContent(String breakpointFileName) throws ScmServerException {
        ScmLockPath breakpointFilelockPath = ScmLockPathFactory.createBPLockPath(ws.getName(),
                breakpointFileName);
        ScmLock breakpointFileXLock = ScmLockManager.getInstance()
                .acquiresLock(breakpointFilelockPath);
        try {
            BreakpointFile breakpointFile = contentserver.getMetaService()
                    .getBreakpointFile(ws.getName(), breakpointFileName);
            if (breakpointFile == null) {
                // TODO:错误码为断点文件不存在
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile is not found: /%s/%s", ws.getName(), breakpointFileName));
            }
            if (!breakpointFile.isCompleted()) {
                throw new ScmInvalidArgumentException(String.format(
                        "Uncompleted BreakpointFile: /%s/%s", ws.getName(), breakpointFileName));
            }

            return updateMeta(breakpointFile.getCreateTime(), breakpointFile.getDataId(),
                    breakpointFile.getUploadSize(), breakpointFile.getSiteId(), breakpointFileName);
        }
        finally {
            unlock(breakpointFileXLock);
        }

    }

    public BSONObject updateContent(InputStream is) throws ScmServerException {
        // check file exist, and check version
        getCurrentFileAndCheckVersion();

        // write data
        Date createDate = new Date();
        String dataId = ScmIdGenerator.FileId.get(createDate);
        ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), dataId, createDate);
        ScmDataWriter dataWriter = null;
        try {
            dataWriter = ScmDataOpFactoryAssit.getFactory().createWriter(
                    contentserver.getLocalSite(), ws.getName(), ws.getDataLocation(),
                    contentserver.getDataService(), dataInfo);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e);
        }
        try {
            write(is, dataWriter);
            dataWriter.close();
        }
        catch (ScmDatasourceException e) {
            dataWriter.cancel();
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to close data writer", e);
        }
        catch (Exception e) {
            dataWriter.cancel();
            throw e;
        }
        finally {
            FileCommonOperator.recordDataTableName(ws.getName(), dataWriter);
        }

        // write meta
        try {
            return updateMeta(createDate.getTime(), dataId, dataWriter.getSize(),
                    contentserver.getLocalSite(), null);
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.COMMIT_UNCERTAIN_STATE) {
                rollbackData(dataInfo);
            }
            throw e;
        }
        catch (Exception e) {
            rollbackData(dataInfo);
            throw e;
        }
    }

    private void write(InputStream is, ScmDataWriter dataWriter) throws ScmServerException {
        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        try {
            while (true) {
                int len = is.read(buf);
                if (len <= -1) {
                    break;
                }
                dataWriter.write(buf, 0, len);
            }
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.FILE_IO,
                    "update file content failed:ws=" + ws.getName() + ",fileId=" + fileId, e);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to update file:ws=" + ws.getName() + ",fileId=" + fileId, e);
        }
    }

    private void rollbackData(ScmDataInfo dataInfo) {
        // delete lob
        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentServer.getInstance().getLocalSite(), ws.getName(),
                    ws.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
            deletor.delete();
        }
        catch (Exception e) {
            logger.warn("rollback file data failed:wsName=" + ws.getName() + ",fileId="
                    + dataInfo.getId(), e);
        }
    }

    // insert historyRec, update currentFileRec, delete breakFileRec, return
    // updated info
    private BSONObject updateMeta(long createTime, String dataId, long dataSize, int siteId,
            String breakFileName) throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(ws.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            BSONObject currentFile = getCurrentFileAndCheckVersion();
            BSONObject historyRec = createHistoryRecord(currentFile);
            BSONObject newVersionUpdator = createNewVersionUpdator(currentFile, dataId, siteId,
                    dataSize, createTime);
            if (breakFileName == null) {
                contentserver.getMetaService().addNewFileVersion(ws.getName(), ws.getMetaLocation(),
                        fileId, historyRec, newVersionUpdator);
            }
            else {
                contentserver.getMetaService().breakpointFileToNewVersionFile(ws.getName(),
                        ws.getMetaLocation(), breakFileName, fileId, historyRec, newVersionUpdator);
            }
            return newVersionUpdator;
        }
        finally {
            writeLock.unlock();
        }
    }

    private BSONObject createNewVersionUpdator(BSONObject currentFile, String dataId, int siteId,
            long size, long createTime) {
        BSONObject newVersionUpdator = new BasicBSONObject();
        // id
        newVersionUpdator.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, dataId);

        // version
        int currentMajorVersion = (int) currentFile.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
        newVersionUpdator.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, currentMajorVersion + 1);

        // size
        newVersionUpdator.put(FieldName.FIELD_CLFILE_FILE_SIZE, size);

        // siteList
        BSONObject sites = new BasicBSONList();
        BSONObject oneSite = new BasicBSONObject();
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, siteId);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, createTime);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, createTime);
        sites.put("0", oneSite);
        newVersionUpdator.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST, sites);

        // data create time
        newVersionUpdator.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, createTime);

        // update user
        newVersionUpdator.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, user);

        // update time
        newVersionUpdator.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, createTime);

        return newVersionUpdator;

    }

    private BSONObject createHistoryRecord(BSONObject currentFile) {
        BSONObject historyRec = new BasicBSONObject();
        historyRec.put(FieldName.FIELD_CLFILE_ID, currentFile.get(FieldName.FIELD_CLFILE_ID));
        historyRec.put(FieldName.FIELD_CLFILE_FILE_DATA_ID,
                currentFile.get(FieldName.FIELD_CLFILE_FILE_DATA_ID));
        historyRec.put(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                currentFile.get(FieldName.FIELD_CLFILE_MAJOR_VERSION));
        historyRec.put(FieldName.FIELD_CLFILE_MINOR_VERSION,
                currentFile.get(FieldName.FIELD_CLFILE_MINOR_VERSION));
        historyRec.put(FieldName.FIELD_CLFILE_FILE_SIZE,
                currentFile.get(FieldName.FIELD_CLFILE_FILE_SIZE));
        historyRec.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST,
                currentFile.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST));
        historyRec.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH,
                currentFile.get(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH));
        historyRec.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME,
                currentFile.get(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME));
        historyRec.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE,
                currentFile.get(FieldName.FIELD_CLFILE_FILE_DATA_TYPE));
        return historyRec;
    }

    private BSONObject getCurrentFileAndCheckVersion() throws ScmServerException {
        BSONObject destFile = contentserver.getMetaService()
                .getCurrentFileInfo(ws.getMetaLocation(), ws.getName(), fileId);
        if (destFile == null) {
            throw new ScmFileNotFoundException(
                    "file not exist:ws=" + ws.getName() + ", fileId=" + fileId);
        }
        if (clientMajorVersion != -1 && clientMinorVersion != -1) {
            int currentMajorVersion = (int) destFile.get(FieldName.FIELD_CLFILE_MAJOR_VERSION);
            int currentMinorVersion = (int) destFile.get(FieldName.FIELD_CLFILE_MINOR_VERSION);
            if (clientMajorVersion != currentMajorVersion
                    || clientMinorVersion != currentMinorVersion) {
                throw new ScmServerException(ScmError.FILE_VERSION_MISMATCHING, "clientFileVersion="
                        + ScmSystemUtils.getVersionStr(clientMajorVersion, clientMinorVersion)
                        + ",currentFileVersion="
                        + ScmSystemUtils.getVersionStr(currentMajorVersion, currentMinorVersion));
            }
        }
        return destFile;
    }

    private void unlock(ScmLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }
}
