package com.sequoiacm.contentserver.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.common.Const;
import com.sequoiacm.contentserver.common.InputStreamWithCalcMd5;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.pipeline.file.module.CreateFileMetaResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileExistStrategy;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaExistException;
import com.sequoiacm.contentserver.pipeline.file.module.FileUploadConf;
import com.sequoiacm.contentserver.pipeline.file.module.OverwriteFileMetaResult;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Component
public class FileCreatorDao {
    private static final Logger logger = LoggerFactory.getLogger(FileCreatorDao.class);
    @Autowired
    private FileMetaOperator fileMetaOperator;
    @Autowired
    private FileOperationListenerMgr listenerMgr;
    @Autowired
    private FileAddVersionDao addVersionDao;

    private FileMeta createMeta(FileUploadConf uploadConf, ScmWorkspaceInfo ws, FileMeta meta,
            TransactionCallback transactionCallback, boolean allowRetry) throws ScmServerException {
        meta = meta.clone();
        FileMeta newFileMeta;
        OperationCompleteCallback operationCompleteCallback = null;
        ScmLock dirLock = ScmFileOperateUtils.lockDirForCreateFile(ws, meta.getDirId());
        try {
            ScmFileOperateUtils.checkDirForCreateFile(ws, meta.getDirId());
            listenerMgr.preCreate(ws, meta);
            CreateFileMetaResult res = fileMetaOperator.createFileMeta(ws.getName(), meta,
                    transactionCallback);
            newFileMeta = res.getNewFile();
            operationCompleteCallback = listenerMgr.postCreate(ws, newFileMeta.getId());
        }
        catch (FileMetaExistException e) {
            if (uploadConf.getExistStrategy() == FileExistStrategy.OVERWRITE) {
                logger.debug(
                        "failed to create file, cause the file already exist, try overwrite file now: ws={}, fileId={}",
                        ws, e.getExistFileId(), e);
                FileInfoAndOpCompleteCallback fileInfoAndCallback = overWriteFile(
                        e.getExistFileId(), ws, meta, transactionCallback);
                newFileMeta = fileInfoAndCallback.getFileInfo();
                operationCompleteCallback = fileInfoAndCallback.getCallback();
            }
            else if (uploadConf.getExistStrategy() == FileExistStrategy.ADD_VERSION) {
                // 新增版本无需持有目录锁
                unlock(dirLock);
                dirLock = null;

                logger.debug(
                        "failed to create file, cause the file already exist, try add version file now: ws={}, fileId={}",
                        ws, e.getExistFileId(), e);
                try {
                    newFileMeta = addVersion(e.getExistFileId(), ws, meta, transactionCallback);
                }
                catch (ScmServerException ex) {
                    if (ex.getError() != ScmError.FILE_NOT_FOUND || !allowRetry) {
                        throw ex;
                    }
                    logger.debug(
                            "failed to add version for the file, cause the file not exist, try create file again: ws={}, fileId={}",
                            ws, e.getExistFileId(), ex);
                    newFileMeta = createMeta(uploadConf, ws, meta, transactionCallback, false);
                }
            }
            else if (uploadConf.getExistStrategy() == FileExistStrategy.THROW_EXCEPTION) {
                throw e;
            }
            else {
                throw new ScmServerException(ScmError.SYSTEM_ERROR,
                        "unknown FileExistStrategy:" + uploadConf.getExistStrategy(), e);
            }
        }
        finally {
            unlock(dirLock);
        }
        if (operationCompleteCallback != null) {
            operationCompleteCallback.onComplete();
        }
        return newFileMeta;
    }

    private void unlock(ScmLock l) {
        if (l != null) {
            l.unlock();
        }
    }

    private FileMeta addVersion(String conflictFileId, ScmWorkspaceInfo ws, FileMeta meta,
            TransactionCallback transactionCallback) throws ScmServerException {
        FileInfoAndOpCompleteCallback ret = addVersionDao.addVersion(FileAddVersionDao.Context
                .standard(ws.getName(), conflictFileId, meta, transactionCallback));
        ret.getCallback().onComplete();
        return ret.getFileInfo();
    }

    private FileInfoAndOpCompleteCallback overWriteFile(String existFileId, ScmWorkspaceInfo ws,
            FileMeta meta,
            TransactionCallback transactionCallback) throws ScmServerException {
        OverwriteFileMetaResult res;
        try {
            res = overwriteFileMeta(existFileId, ws, meta, transactionCallback);
        }
        catch (FileMetaExistException e) {
            logger.info(
                    "failed to overwrite file, cause the conflict file id is change, try overwrite again: oldConflictId={}, newConflictId={}, workspace={}",
                    existFileId, e.getExistFileId(), ws.getName(), e);
            res = overwriteFileMeta(e.getExistFileId(), ws, meta, transactionCallback);
        }
        catch (BatchChangeException e) {
            logger.info(
                    "failed to overwrite file, cause the conflict file batch lock timeout, try overwrite again: conflictId={}, workspace={}",
                    existFileId, ws.getName(), e);
            res = overwriteFileMeta(existFileId, ws, meta, transactionCallback);
        }

        OperationCompleteCallback operationCompleteCallback = listenerMgr.postCreate(ws,
                res.getNewFile().getId());
        if (!res.getDeletedVersion().isEmpty()) {
            listenerMgr.postDelete(ws, res.getDeletedVersion());
        }


        // delete file data async
        OverwriteFileMetaResult finalRes = res;
        AsyncUtils.execute(() -> {
            for (FileMeta fileRecord : finalRes.getDeletedVersion()) {
                ScmFileDataDeleterWrapper dataDeleter = new ScmFileDataDeleterWrapper(ws,
                        fileRecord);
                dataDeleter.deleteDataSilence();
            }
        });
        return new FileInfoAndOpCompleteCallback(res.getNewFile(), operationCompleteCallback);
    }

    private OverwriteFileMetaResult overwriteFileMeta(String existFileId, ScmWorkspaceInfo ws,
            FileMeta meta, TransactionCallback transactionCallback) throws ScmServerException {
        BSONObject overwrittenFileBson = ScmContentModule.getInstance().getCurrentFileInfo(ws,
                existFileId, true);
        if (overwrittenFileBson == null) {
            CreateFileMetaResult res = fileMetaOperator.createFileMeta(ws.getName(), meta,
                    transactionCallback);
            return new OverwriteFileMetaResult(res.getNewFile(), Collections.emptyList());
        }
        FileMeta overwrittenFile = FileMeta.fromRecord(overwrittenFileBson);
        ScmLock batchLock = null;
        ScmLock fileLock = null;
        try {
            if (overwrittenFile.getBatchId() != null && !overwrittenFile.getBatchId().isEmpty()) {
                ScmLockPath batchLockPath = ScmLockPathFactory.createBatchLockPath(ws.getName(),
                        overwrittenFile.getBatchId());
                batchLock = ScmLockManager.getInstance().acquiresLock(batchLockPath);
            }
            ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(ws.getName(),
                    existFileId);
            fileLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);

            overwrittenFileBson = ScmContentModule.getInstance().getCurrentFileInfo(ws, existFileId,
                    true);
            if (overwrittenFileBson == null) {
                CreateFileMetaResult res = fileMetaOperator.createFileMeta(ws.getName(), meta,
                        transactionCallback);
                return new OverwriteFileMetaResult(res.getNewFile(), Collections.emptyList());
            }
            FileMeta overwrittenFileInLock = FileMeta.fromRecord(overwrittenFileBson);
            if (!isConflictFileBatchSame(overwrittenFile, overwrittenFileInLock)) {
                throw new BatchChangeException("file batch id change ws=" + ws.getName()
                        + ", oldBatch=" + overwrittenFile.getBatchId() + ", newBatch="
                        + overwrittenFileInLock.getBatchId() + ", file=" + overwrittenFile.getId());
            }
            return fileMetaOperator.overwriteFileMeta(ws.getName(), meta, transactionCallback,
                    overwrittenFileInLock);

        }
        finally {
            if (fileLock != null) {
                fileLock.unlock();
            }
            if (batchLock != null) {
                batchLock.unlock();
            }
        }
    }

    private boolean isConflictFileBatchSame(FileMeta conflictFile, FileMeta conflictFileInLock) {
        return Objects.equals(conflictFile.getBatchId(), conflictFileInLock.getBatchId());
    }

    @SlowLog(operation = "createFile")
    public FileMeta createFile(String workspaceName, FileMeta fileMeta, FileUploadConf uploadConf,
            TransactionCallback transactionCallback) throws ScmServerException {
        IdInfo idInfo = generateFileId(fileMeta);
        fileMeta.resetFileIdAndFileTime(idInfo.fileId, idInfo.fileCreateDate);
        return createMeta(uploadConf,
                ScmContentModule.getInstance().getWorkspaceInfoCheckExist(workspaceName), fileMeta,
                transactionCallback, true);
    }

    @SlowLog(operation = "createFile", extras = @SlowLogExtra(name = "breakpointFileName", data = "breakpointFileName"))
    public FileMeta createFile(String workspaceName, FileMeta fileMeta, FileUploadConf uploadConf,
            String breakpointFileName) throws ScmServerException {
        IdInfo idInfo = generateFileId(fileMeta);
        fileMeta.resetFileIdAndFileTime(idInfo.fileId, idInfo.fileCreateDate);

        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        OperationCompleteCallback callback;
        FileMeta res;
        ScmLockPath lockPath = ScmLockPathFactory.createBPLockPath(workspaceName,
                breakpointFileName);
        ScmLock lock = ScmLockManager.getInstance().acquiresLock(lockPath);

        try {
            BreakpointFile breakpointFile = contentModule.getMetaService()
                    .getBreakpointFile(workspaceName, breakpointFileName);
            if (breakpointFile == null) {
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile is not found: /%s/%s", workspaceName, breakpointFileName));
            }

            if (breakpointFile.getSiteId() != contentModule.getLocalSite()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile[/%s/%s] should be uploaded in site[%s]",
                                workspaceName, breakpointFileName, breakpointFile.getSiteName()));
            }

            if (!breakpointFile.isCompleted()) {
                throw new ScmInvalidArgumentException(String.format(
                        "Uncompleted BreakpointFile: /%s/%s", workspaceName, breakpointFileName));
            }

            if (uploadConf.isNeedMd5() && !breakpointFile.isNeedMd5()) {
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile has no md5: /%s/%s", workspaceName, breakpointFileName));
            }

            if (fileMeta.getName() == null) {
                fileMeta.setName(breakpointFileName);
            }

            fileMeta.resetDataInfo(breakpointFile.getDataId(), breakpointFile.getCreateTime(),
                    ENDataType.Normal.getValue(), breakpointFile.getUploadSize(),
                    breakpointFile.getMd5(), contentModule.getLocalSite(),
                    breakpointFile.getWsVersion(), breakpointFile.getTableName());
            res = createMeta(uploadConf, wsInfo, fileMeta, (context) -> {
                ScmContentModule.getInstance().getMetaService()
                        .deleteBreakpointFile(wsInfo.getName(), breakpointFileName, context);
            }, true);
            callback = listenerMgr.postCreate(wsInfo, res.getId());
        }
        finally {
            lock.unlock();
        }
        callback.onComplete();
        return res;
    }

    class IdInfo {
        String fileId;
        String dataId;
        Date fileCreateDate;

        IdInfo(String fileId, String dataId, Date fileCreateDate) {
            this.fileId = fileId;
            this.dataId = dataId;
            this.fileCreateDate = fileCreateDate;
        }
    }

    public IdInfo generateFileId(FileMeta fileInfo) throws ScmServerException {
        String fileId = fileInfo.getId();
        Date fileCreateDate = new Date();
        String dataId;

        if (fileId == null) {
            if (fileInfo.getCreateTime() != null) {
                fileCreateDate = new Date(fileInfo.getCreateTime());
            }
            fileId = ScmIdGenerator.FileId.get(fileCreateDate);
            dataId = fileId;
        }
        else {
            ScmIdParser parser = new ScmIdParser(fileId);
            if (fileInfo.getCreateTime() != null) {
                fileCreateDate = new Date(fileInfo.getCreateTime());
            }
            else {
                fileCreateDate = new Date(parser.getSecond() * 1000);
            }
            dataId = ScmIdGenerator.FileId.get(new Date(System.currentTimeMillis()));
        }
        return new IdInfo(fileId, dataId, fileCreateDate);
    }

    @SlowLog(operation = "createFile")
    public FileMeta createFile(String ws, FileMeta fileMeta, FileUploadConf conf, InputStream is)
            throws ScmServerException {
        IdInfo idInfo = generateFileId(fileMeta);
        fileMeta.resetFileIdAndFileTime(idInfo.fileId, idInfo.fileCreateDate);
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(ws);
        ScmDataInfo dataInfo = ScmDataInfo.forCreateNewData(ENDataType.Normal.getValue(),
                idInfo.dataId,
                idInfo.fileCreateDate, wsInfo.getVersion());

        boolean hasCreateData = false;

        ScmDataWriter fileWriter;
        ScmDataWriterContext context = new ScmDataWriterContext();
        try {
            fileWriter = ScmDataOpFactoryAssit.getFactory().createWriter(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()), contentModule.getDataService(),
                    dataInfo, context);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "failed to create data writer", e);
        }
        String md5 = null;
        try {
            if (!conf.isNeedMd5()) {
                writeData(dataInfo, fileWriter, is);
            }
            else {
                InputStreamWithCalcMd5 md5Is = new InputStreamWithCalcMd5(is, false);
                try {
                    writeData(dataInfo, fileWriter, md5Is);
                    md5 = md5Is.calcMd5();
                }
                finally {
                    ScmSystemUtils.closeResource(md5Is);
                }
            }
            commitWriter(fileWriter, ws);
            String tableName = context.getTableName();
            dataInfo.setTableName(context.getTableName());
            fileMeta.resetDataInfo(dataInfo.getId(), dataInfo.getCreateTime().getTime(),
                    dataInfo.getType(), fileWriter.getSize(), md5,
                    ScmContentModule.getInstance().getLocalSite(), dataInfo.getWsVersion(),
                    tableName);
            hasCreateData = true;
            return createMeta(conf, wsInfo, fileMeta, null, true);
        }
        catch (Exception e) {
            if (!hasCreateData) {
                cancelWriter(fileWriter, ws);
            }
            if (hasCreateData && e instanceof ScmServerException) {
                ScmServerException scmServerException = (ScmServerException) e;
                if (scmServerException.getError() != ScmError.COMMIT_UNCERTAIN_STATE) {
                    removeLocalDataSilence(wsInfo, dataInfo);
                }
            }
            throw e;

        }
    }

    private void removeLocalDataSilence(ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo) {
        try {
            ScmDataDeletor deleter = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(dataInfo.getWsVersion()), ScmContentModule.getInstance().getDataService(),
                    dataInfo);
            deleter.delete();
        }
        catch (Exception e) {
            logger.warn("delete file data failed: wsName=" + wsInfo.getName() + ",dataId="
                    + dataInfo.getId(), e);
        }
    }

    @SlowLog(operation = "writeFileData")
    private void writeData(ScmDataInfo dataInfo, ScmDataWriter dataWriter, InputStream data)
            throws ScmServerException {
        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        try {
            while (true) {
                int len = CommonHelper.readAsMuchAsPossible(data, buf);
                if (len <= -1) {
                    break;
                }

                dataWriter.write(buf, 0, len);

                if (len < buf.length) {
                    break;
                }
            }
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.FILE_IO,
                    "write file data failed: dataId=" + dataInfo.getId(), e);
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_WRITE_ERROR),
                    "write file failed: dataId=" + dataInfo.getId(), e);
        }
        finally {
            // GC may be more efficiently
            buf = null;
        }
    }

    private void commitWriter(ScmDataWriter fileWriter, String wsName) throws ScmServerException {
        FileCommonOperator.closeWriter(fileWriter);
        FileCommonOperator.recordDataTableName(wsName, fileWriter);
    }

    public void cancelWriter(ScmDataWriter fileWriter, String wsName) {
        FileCommonOperator.cancelWriter(fileWriter);
        FileCommonOperator.recordDataTableName(wsName, fileWriter);
    }

}

class BatchChangeException extends ScmServerException {
    public BatchChangeException(String message) {
        super(ScmError.RESOURCE_CONFLICT, message);
    }
}
