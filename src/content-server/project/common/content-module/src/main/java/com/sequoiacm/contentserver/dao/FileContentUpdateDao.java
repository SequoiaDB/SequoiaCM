package com.sequoiacm.contentserver.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.InputStreamWithCalcMd5;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaBreakpointFileAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;

@Component
public class FileContentUpdateDao {
    private static final Logger logger = LoggerFactory.getLogger(FileContentUpdateDao.class);
    @Autowired
    private BucketInfoManager bucketInfoMgr;
    @Autowired
    private FileAddVersionDao fileAddVersionDao;

    public FileMeta updateContent(String user, String wsName, String fileId, int clientMajorVersion,
            int clientMinorVersion, ScmUpdateContentOption option, String breakpointFileName)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(wsName);
        ScmLockPath breakpointFilelockPath = ScmLockPathFactory.createBPLockPath(wsName,

                breakpointFileName);
        ScmLock breakpointFileXLock = ScmLockManager.getInstance()
                .acquiresLock(breakpointFilelockPath);
        try {
            BreakpointFile breakpointFile =contentModule.getMetaService()
                    .getBreakpointFile(wsName, breakpointFileName);

            if (breakpointFile == null) {
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile is not found: /%s/%s", wsName, breakpointFileName));
            }
            if (!breakpointFile.isCompleted()) {
                throw new ScmInvalidArgumentException(String.format(
                        "Uncompleted BreakpointFile: /%s/%s", wsName, breakpointFileName));
            }

            BSONObject currentLatestVersion = getCurrentFileAndCheckVersion(wsInfo, fileId,
                    clientMajorVersion, clientMinorVersion);
            Number bucketId = BsonUtils.getNumber(currentLatestVersion,
                    FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
            if (bucketId != null && bucketInfoMgr.getBucketById(bucketId.longValue()) != null) {
                option.setNeedMd5(true);
            }

            if (option.isNeedMd5() && !breakpointFile.isNeedMd5()) {
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile has no md5: /%s/%s", wsName, breakpointFileName));
            }


            return updateMeta(wsInfo, user, fileId, breakpointFile.getCreateTime(),
                    breakpointFile.getDataId(), breakpointFile.getUploadSize(),
                    breakpointFile.getSiteId(), breakpointFileName, breakpointFile.getMd5(),
                    breakpointFile.getWsVersion(), breakpointFile.getTableName());
        }
        finally {
            unlock(breakpointFileXLock);
        }

    }

    public FileMeta updateContent(String user, String wsName, String fileId, int clientMajorVersion,
            int clientMinorVersion, ScmUpdateContentOption option, InputStream is)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckExist(wsName);
        // check file exist, and check version
        BSONObject currentLatestVersion = getCurrentFileAndCheckVersion(ws, fileId,
                clientMajorVersion, clientMinorVersion);
        Number bucketId = BsonUtils.getNumber(currentLatestVersion,
                FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (bucketId != null && bucketInfoMgr.getBucketById(bucketId.longValue()) != null) {
            option.setNeedMd5(true);
        }

        // write data
        Date createDate = new Date();
        String dataId = ScmIdGenerator.FileId.get(createDate);
        ScmDataInfo dataInfo = ScmDataInfo.forCreateNewData(ENDataType.Normal.getValue(), dataId,
                createDate,
                ws.getVersion());
        ScmDataWriter dataWriter = null;
        ScmDataWriterContext context = new ScmDataWriterContext();
        try {
            dataWriter = ScmDataOpFactoryAssit.getFactory().createWriter(
                    contentModule.getLocalSite(), ws.getName(),
                    ws.getDataLocation(dataInfo.getWsVersion()), contentModule.getDataService(),
                    dataInfo, context);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to create data writer", e);
        }

        String md5 = null;
        if (!option.isNeedMd5()) {
            writeData(wsName, fileId, is, dataWriter);
        }
        else {
            InputStreamWithCalcMd5 md5Is = new InputStreamWithCalcMd5(is, false);
            try {
                writeData(wsName, fileId, md5Is, dataWriter);
                md5 = md5Is.calcMd5();
            }
            finally {
                ScmSystemUtils.closeResource(md5Is);
            }
        }

        dataInfo.setTableName(context.getTableName());
        // write meta
        try {
            return updateMeta(ws, user, fileId, createDate.getTime(), dataId, dataWriter.getSize(),
                    contentModule.getLocalSite(), null, md5, dataInfo.getWsVersion(), context.getTableName());
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.COMMIT_UNCERTAIN_STATE) {
                rollbackData(ws, dataInfo);
            }
            throw e;
        }
        catch (Exception e) {
            rollbackData(ws, dataInfo);
            throw e;
        }
    }

    private void writeData(String ws, String fileId, InputStream is, ScmDataWriter dataWriter)
            throws ScmServerException {
        try {
            write(ws, fileId, is, dataWriter);
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
            FileCommonOperator.recordDataTableName(ws, dataWriter);
        }
    }

    private void write(String ws, String fileId, InputStream is, ScmDataWriter dataWriter)
            throws ScmServerException {
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
                    "update file content failed:ws=" + ws + ",fileId=" + fileId, e);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "Failed to update file:ws=" + ws + ",fileId=" + fileId, e);
        }
    }

    private void rollbackData(ScmWorkspaceInfo ws, ScmDataInfo dataInfo) {
        // delete lob
        try {
            ScmDataDeletor deletor = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), ws.getName(),
                    ws.getDataLocation(dataInfo.getWsVersion()), ScmContentModule.getInstance().getDataService(),
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
    private FileMeta updateMeta(ScmWorkspaceInfo ws, String user, String fileId, long createTime,
            String dataId, long dataSize, int siteId, final String breakFileName, String md5,
            int wsVersion, String tableName) throws ScmServerException {
        FileInfoAndOpCompleteCallback ret = null;
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(ws.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            BSONObject latestFileVersionInLock = ScmContentModule.getInstance().getMetaService()
                    .getFileInfo(ws.getMetaLocation(), ws.getName(), fileId, -1, -1);
            if (latestFileVersionInLock == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file not exist: ws=" + ws.getName() + ", fileId=" + fileId);
            }

            FileMeta newVersion = createNewVersionMeta(ws.getName(), user, latestFileVersionInLock,
                    dataId, siteId, dataSize, createTime, md5, wsVersion, tableName);

            TransactionCallback transactionCallback = null;
            if (breakFileName != null) {
                transactionCallback = new TransactionCallback() {
                    @Override
                    public void beforeTransactionCommit(TransactionContext context)
                            throws ScmServerException, ScmMetasourceException {
                        MetaBreakpointFileAccessor breakpointAccessor = ScmContentModule
                                .getInstance()
                                .getMetaService().getMetaSource()
                                .getBreakpointFileAccessor(ws.getName(), context);
                        breakpointAccessor.delete(breakFileName);
                    }
                };
            }
            ret = fileAddVersionDao
                    .addVersion(FileAddVersionDao.Context.lockInCaller(ws.getName(), newVersion,
                            FileMeta.fromRecord(latestFileVersionInLock), transactionCallback));
        }
        finally {
            writeLock.unlock();
        }

        ret.getCallback().onComplete();
        return ret.getFileInfo();

    }

    private FileMeta createNewVersionMeta(String ws, String user, BSONObject currentLatestVersion,
            String dataId, int siteId, long size, long createTime, String md5, int wsVersion,
            String tableName) throws ScmServerException {
        // currentLatestVersion 来自表中存在的文件元数据，无需检查 classProperties （同时有可能该文件的元数据模型已被删除，若检查可能会不通过）
        FileMeta fileMeta = FileMeta.fromUser(ws, currentLatestVersion, user, false);
        fileMeta.resetDataInfo(dataId, createTime, ENDataType.Normal.getValue(), size, md5, siteId,
                wsVersion, tableName);
        return fileMeta;
    }

    private BSONObject getCurrentFileAndCheckVersion(ScmWorkspaceInfo ws, String fileId,
            int clientMajorVersion, int clientMinorVersion) throws ScmServerException {
        BSONObject destFile = ScmContentModule.getInstance().getMetaService()
                .getCurrentFileInfo(ws.getMetaLocation(), ws.getName(), fileId, false);
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
