package com.sequoiacm.contentserver.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdaterFactoryWrapper;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaResult;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileInfoUpdaterDao {
    private static final Logger logger = LoggerFactory.getLogger(FileInfoUpdaterDao.class);
    @Autowired
    private FileMetaOperator fileMetaOperator;
    @Autowired
    private FileOperationListenerMgr listenerMgr;

    @Autowired
    private FileMetaUpdaterFactoryWrapper fileMetaUpdaterFactory;

    @Autowired
    private FileMetaFactory fileMetaFactory;

    public FileMeta updateInfo(String user, String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject updator) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(workspaceName);
        logger.debug("updating file:wsName=" + ws.getName() + ",fileId=" + fileId + ",version="
                + ScmSystemUtils.getVersionStr(majorVersion, minorVersion) + ",new="
                + updator.toString());

        List<FileMetaUpdater> fileMetaUpdaterList = new ArrayList<>();
        for (String key : updator.keySet()) {
            FileMetaUpdater fileMetaUpdater = fileMetaUpdaterFactory.createFileMetaUpdater(ws, key,
                    updator.get(key), majorVersion, minorVersion);
            fileMetaUpdaterList.add(fileMetaUpdater);
        }

        Date updateDate = new Date();
        FileMeta updatedFileMeta = null;
        OperationCompleteCallback callback = null;
        ScmLock batchLock = null;
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(ws.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            BSONObject currentLatestVersionBSON = ScmContentModule.getInstance().getMetaService()
                    .getFileInfo(ws.getMetaLocation(), ws.getName(), fileId, -1, -1);
            if (currentLatestVersionBSON == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file not found: ws=" + ws.getName() + ", fileId=" + fileId);
            }
            FileMeta currentLatestVersion = fileMetaFactory.createFileMetaByRecord(workspaceName,
                    currentLatestVersionBSON);
            if (needCheckUniqueNameInBatch(ws, currentLatestVersion, updator)) {
                ScmLockPath batchLockPath = ScmLockPathFactory.createBatchLockPath(ws.getName(),
                        currentLatestVersion.getBatchId());
                batchLock = ScmLockManager.getInstance().acquiresLock(batchLockPath,
                        PropertiesUtils.getServerConfig().getFileRenameBatchLockTimeout());
                if (batchLock == null) {
                    throw new ScmServerException(ScmError.OPERATION_TIMEOUT,
                            "acquires batch lock timeout:ws=" + ws.getName() + ", batch="
                                    + currentLatestVersion.getBatchId() + ", file=" + fileId);
                }
                checkUniqueFileNameInBatch(ws, currentLatestVersion.getBatchId(),
                        (String) updator.get(FieldName.FIELD_CLFILE_NAME));
            }

            UpdateFileMetaResult res = fileMetaOperator.updateFileMeta(ws.getName(), fileId,
                    fileMetaUpdaterList, user, updateDate, currentLatestVersion,
                    new ScmVersion(majorVersion, minorVersion));

            callback = listenerMgr.postUpdate(ws, currentLatestVersion,
                    res.getLatestVersionAfterUpdate());
            updatedFileMeta = res.getSpecifiedReturnVersion();
        }
        finally {
            if (batchLock != null) {
                batchLock.unlock();
            }
            writeLock.unlock();
        }
        callback.onComplete();
        return updatedFileMeta;
    }

    private void checkUniqueFileNameInBatch(ScmWorkspaceInfo ws, String batchId, String newFileName)
            throws ScmServerException {
        String batchCreateMonth = ScmSystemUtils.getCreateMonthFromBatchId(ws, batchId);
        BSONObject batch = ScmContentModule.getInstance().getMetaService().getBatchInfo(ws, batchId,
                batchCreateMonth);
        if (batch == null) {
            return;
        }
        BasicBSONList files = BsonUtils.getArray(batch, FieldName.Batch.FIELD_FILES);
        if (files == null || files.size() <= 1) {
            return;
        }
        BasicBSONObject condition = new BasicBSONObject();
        ArrayList<String> ids = new ArrayList<>(files.size());
        for (Object file : files) {
            BSONObject fileBson = (BSONObject) file;
            ids.add(BsonUtils.getStringChecked(fileBson, FieldName.FIELD_CLFILE_ID));
        }
        BasicBSONObject dollarInFileIds = new BasicBSONObject("$in", ids);
        condition.put(FieldName.FIELD_CLFILE_ID, dollarInFileIds);

        ArrayList<String> fileCreateMonths = new ArrayList<>(files.size());
        for (String id : ids) {
            ScmIdParser idParser = new ScmIdParser(id);
            fileCreateMonths.add(idParser.getMonth());
        }
        BasicBSONObject dollarInCreateMonths = new BasicBSONObject("$in", fileCreateMonths);
        condition.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, dollarInCreateMonths);

        condition.put(FieldName.FIELD_CLFILE_NAME, newFileName);
        long sameNameFileCount = ScmContentModule.getInstance().getMetaService()
                .getCurrentFileCount(ws, condition);
        if (sameNameFileCount > 0) {
            throw new ScmServerException(ScmError.BATCH_FILE_SAME_NAME,
                    "rename file failed, the batch already attach a file with same name:ws="
                            + ws.getName() + ", batch=" + batchId + ", fileName=" + newFileName);
        }
    }

    private boolean needCheckUniqueNameInBatch(ScmWorkspaceInfo ws, FileMeta currentLatestVersion,
            BSONObject updator) {
        String batchId = currentLatestVersion.getBatchId();
        if (batchId == null || batchId.length() <= 0) {
            return false;
        }

        if (!ws.isBatchFileNameUnique()) {
            return false;
        }

        String newFileName = (String) updator.get(FieldName.FIELD_CLFILE_NAME);
        if (newFileName == null) {
            return false;
        }

        if (newFileName.equals(currentLatestVersion.getName())) {
            return false;
        }

        return true;

    }
}
