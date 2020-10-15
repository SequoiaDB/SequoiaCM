package com.sequoiacm.contentserver.service.impl;

import java.util.ArrayList;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.IBatchDao;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathDefine;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.service.IBatchService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaCursor;

@Service
public class BatchServiceImpl implements IBatchService {

    private static final Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);

    @Autowired
    private IBatchDao batchDao;

    @Autowired
    private FileOperationListenerMgr fileOpListenerMgr;

    @Override
    public BSONObject getBatchInfo(String workspaceName, String batchId, boolean isDetail)
            throws ScmServerException {
        BSONObject batch = null;
        try {
            ScmWorkspaceInfo wsInfo = getWorkspace(workspaceName);

            String batchCreateMonth = ScmSystemUtils.getCreateMonthFromBatchId(wsInfo, batchId);
            batch = getAndCheckBatch(wsInfo, batchId, batchCreateMonth);

            if (isDetail) {
                // get file info by file id
                BasicBSONList fileIds = (BasicBSONList) batch.get(FieldName.Batch.FIELD_FILES);
                BasicBSONList fileInfos = new BasicBSONList();
                for (Object obj : fileIds) {
                    BSONObject file = (BSONObject) obj;
                    String fileId = (String) file.get(FieldName.FIELD_CLFILE_ID);
                    BSONObject fileInfo = ScmContentServer.getInstance().getCurrentFileInfo(wsInfo,
                            fileId);
                    // if the file is unexist, skip.
                    if (null == fileInfo) {
                        continue;
                    }
                    fileInfo.removeField("_id");
                    fileInfos.add(fileInfo);
                }
                // associate file detail info
                batch.put(FieldName.Batch.FIELD_FILES, fileInfos);
            }
        }
        catch (Exception e) {
            logger.error("getBatchInfo failed: workspace={},batchId={}", workspaceName, batchId);
            throw e;
        }
        return batch;
    }

    @Override
    public MetaCursor getList(String workspaceName, BSONObject matcher, BSONObject orderBy,
            long skip, long limit) throws ScmServerException {
        try {
            ScmWorkspaceInfo wsInfo = getWorkspace(workspaceName);
            return batchDao.query(wsInfo, matcher, orderBy, skip, limit);
        }
        catch (ScmServerException e) {
            throw e;
        }
    }

    @Override
    public void delete(String sessionId, String userDetail, String user, String workspaceName,
            String batchId) throws ScmServerException {
        ScmLock batchLock = null;
        ScmLockPath batchLockPath = null;

        try {
            ScmWorkspaceInfo wsInfo = getWorkspace(workspaceName);
            String batchCreateMonth = ScmSystemUtils.getCreateMonthFromBatchId(wsInfo, batchId);
            // lock
            try {
                batchLockPath = ScmLockPathFactory.createBatchLockPath(wsInfo.getName(), batchId);
                batchLock = lock(batchLockPath);
            }
            catch (Exception e) {
                throw new ScmSystemException(
                        "delete failed, an error occurs during get batch lock: workspace="
                                + workspaceName + ",batchId=" + batchId,
                        e);
            }

            // delete batch
            batchDao.delete(wsInfo, batchId, batchCreateMonth, sessionId, userDetail, user);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("delete failed: workspace={},batchId={}", workspaceName, batchId);
            throw e;
        }
        finally {
            unlock(batchLock, batchLockPath);
        }
    }

    @Override
    public String create(String user, String workspaceName, BSONObject batchInfo)
            throws ScmServerException {
        try {
            ScmWorkspaceInfo wsInfo = getWorkspace(workspaceName);
            return batchDao.insert(wsInfo, batchInfo, user);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("create failed: workspace={},batchInfo={}", workspaceName, batchInfo);
            throw e;
        }
    }

    @Override
    public void update(String user, String workspaceName, String batchId, BSONObject updator)
            throws ScmServerException {

        ScmLock batchLock = null;
        ScmLockPath batchLockPath = null;

        try {
            ScmWorkspaceInfo wsInfo = getWorkspace(workspaceName);
            String batchCreateMonth = ScmSystemUtils.getCreateMonthFromBatchId(wsInfo, batchId);
            // lock
            try {
                batchLockPath = ScmLockPathFactory.createBatchLockPath(wsInfo.getName(), batchId);
                batchLock = lock(batchLockPath);
            }
            catch (Exception e) {
                throw new ScmSystemException(
                        "update failed, an error occurs during get batch lock: lockPath="
                                + batchLockPath,
                        e);
            }

            boolean ret = batchDao.updateById(wsInfo, batchId, batchCreateMonth, updator, user);
            if (!ret) {
                throw new ScmServerException(ScmError.BATCH_NOT_FOUND,
                        "update failed, batch is unexist: workspace=" + workspaceName + ", batchId="
                                + batchId);
            }
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("update failed: workspace={},batchId={},updator={}", workspaceName,
                    batchId, updator);
            throw e;
        }
        finally {
            unlock(batchLock, batchLockPath);
        }
    }

    @Override
    public void attachFile(String user, String workspaceName, String batchId, String fileId)
            throws ScmServerException {

        ScmLock batchLock = null;
        ScmLockPath batchLockPath = null;
        ScmLock fileLock = null;
        ScmLockPath fileLockPath = null;
        OperationCompleteCallback callback = null;
        try {
            ScmWorkspaceInfo wsInfo = getWorkspace(workspaceName);
            String batchCreateMonth = ScmSystemUtils.getCreateMonthFromBatchId(wsInfo, batchId);

            String lockFlag = "";
            try {
                // batch lock
                batchLockPath = ScmLockPathFactory.createBatchLockPath(wsInfo.getName(), batchId);
                lockFlag = ScmLockPathDefine.BATCHES;
                batchLock = lock(batchLockPath);
                // file lock
                fileLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId);
                lockFlag = ScmLockPathDefine.FILES;
                fileLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);
            }
            catch (Exception e) {
                throw new ScmSystemException("attachFile failed, an error occurs during get "
                        + lockFlag + " lock: batchLockPath=" + batchLockPath + ", fileLockPath="
                        + fileLockPath, e);
            }

            BSONObject batch = getAndCheckBatch(wsInfo, batchId, batchCreateMonth);
            BSONObject fileInfo = ScmContentServer.getInstance().getCurrentFileInfo(wsInfo, fileId);
            // file unexist
            if (null == fileInfo) {
                throw new ScmFileNotFoundException("attachFile failed, file is unexist: workspace="
                        + workspaceName + ",batchId=" + batchId + ",fileId=" + fileId);
            }
            String batchIdInFile = (String) fileInfo.get(FieldName.FIELD_CLFILE_BATCH_ID);
            // batch has attach file
            if (isFileInBatch(batch, fileId)) {
                if (StringUtils.isEmpty(batchIdInFile)) {
                    // update file's batch_id
                    ScmContentServer.getInstance().getMetaService()
                            .updateBatchIdOfFile(workspaceName, batchId, fileId, user, null);
                }
                throw new ScmServerException(ScmError.FILE_IN_SPECIFIED_BATCH,
                        "attachFile failed, the file is already in the batch: workspace="
                                + workspaceName + ", batchId=" + batchId + ", fileId=" + fileId);
            }

            // file is in one batch
            if (!StringUtils.isEmpty(batchIdInFile)) {
                if (!batchId.equals(batchIdInFile)) {
                    throw new ScmServerException(ScmError.FILE_IN_ANOTHER_BATCH,
                            "attach file is already in another batch: workspace=" + workspaceName
                                    + ",fileId=" + fileId + ",otherBatchId=" + batchIdInFile);
                }
                // else batchId = batchIdInFile, but there is no such file in
                // the batch.
                // it could be an exception occurred during execute
                // detachFile(), just attach again.
            }

            if (wsInfo.isBatchFileNameUnique()) {
                checkFileNameUnique(wsInfo, batchId, batch, fileInfo);
            }

            // attach
            batchDao.attachFile(wsInfo, batchId, batchCreateMonth, fileId, user);
            callback = fileOpListenerMgr.postUpdate(wsInfo, fileInfo);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("attachFile failed: workspace={},batchId={},fileId={}", workspaceName,
                    batchId, fileId);
            throw e;
        }
        finally {
            unlock(fileLock, fileLockPath);
            unlock(batchLock, batchLockPath);
        }
        callback.onComplete();
    }

    private void checkFileNameUnique(ScmWorkspaceInfo wsInfo, String batchId, BSONObject batch,
            BSONObject fileInfo) throws ScmServerException {
        BasicBSONList files = (BasicBSONList) batch.get(FieldName.Batch.FIELD_FILES);
        if (files != null && files.size() != 0) {
            BasicBSONObject condition = new BasicBSONObject();
            ArrayList<String> ids = new ArrayList<>(files.size());
            for (Object file : files) {
                BSONObject fileBson = (BSONObject) file;
                ids.add(BsonUtils.getStringChecked(fileBson, FieldName.FIELD_CLFILE_ID));
            }

            BasicBSONObject dollarInIds = new BasicBSONObject("$in", ids);
            condition.put(FieldName.FIELD_CLFILE_ID, dollarInIds);

            ArrayList<String> fileCreateMonths = new ArrayList<>(files.size());
            for (String id : ids) {
                ScmIdParser idParser = new ScmIdParser(id);
                fileCreateMonths.add(idParser.getMonth());
            }
            BasicBSONObject dollarInCreateMonths = new BasicBSONObject("$in", fileCreateMonths);
            condition.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, dollarInCreateMonths);

            String fileName = (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME);
            condition.put(FieldName.FIELD_CLFILE_NAME, fileName);

            // condition= {id: {$in: [id1, id2]}, create_month:{$in:
            // ["202001"]}, name: "fileName"}
            long sameNameFileCount = ScmContentServer.getInstance().getMetaService()
                    .getCurrentFileCount(wsInfo, condition);
            if (sameNameFileCount > 0) {
                throw new ScmServerException(ScmError.BATCH_FILE_SAME_NAME,
                        "the batch already attach a file with same name:ws=" + wsInfo.getName()
                                + ", batch=" + batchId + ", fileName=" + fileName);
            }
        }
    }

    @Override
    public void detachFile(String user, String workspaceName, String batchId, String fileId)
            throws ScmServerException {

        ScmLock batchLock = null;
        ScmLockPath batchLockPath = null;

        ScmLock fileLock = null;
        ScmLockPath fileLockPath = null;
        OperationCompleteCallback callback = null;
        try {
            ScmWorkspaceInfo wsInfo = getWorkspace(workspaceName);
            String batchCreateMonth = ScmSystemUtils.getCreateMonthFromBatchId(wsInfo, batchId);
            // lock
            try {
                batchLockPath = ScmLockPathFactory.createBatchLockPath(wsInfo.getName(), batchId);
                batchLock = lock(batchLockPath);

                // file lock
                fileLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId);
                fileLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);
            }
            catch (Exception e) {
                throw new ScmSystemException(
                        "detachFile failed, an error occurs during get lock: workspace="
                                + workspaceName + ",batchId=" + batchId + ",fileId=" + fileId,
                        e);
            }

            BSONObject batch = getAndCheckBatch(wsInfo, batchId, batchCreateMonth);
            BSONObject fileInfo = ScmContentServer.getInstance().getCurrentFileInfo(wsInfo, fileId);
            String batchIdInFile = null;
            if (null != fileInfo) {
                batchIdInFile = (String) fileInfo.get(FieldName.FIELD_CLFILE_BATCH_ID);
            }
            // file unexist
            /*
             * if (null == fileInfo) { throw new
             * ScmServerException(ScmError.METASOURCE_RECORD_NOT_EXIST,
             * "attachFile failed, file is unexist: workspace=" + workspaceName
             * + ",batchId=" + batchId + ",fileId=" + fileId); }
             */

            // when file not in the batch
            if (!isFileInBatch(batch, fileId)) {
                if (null != batchIdInFile && batchId.equals(batchIdInFile)) {
                    // batchId = batchIdInFile, but there is no such file in the
                    // batch.
                    // it could be an exception occurred during execute
                    // attachFile(), just detach again.
                }
                else {
                    throw new ScmServerException(ScmError.FILE_NOT_IN_BATCH,
                            "detachFile failed, the file is not in the batch: workspace="
                                    + workspaceName + "batchId=" + batchId + ",fileId=" + fileId);
                }
            }

            // detach
            batchDao.detachFile(wsInfo, batchId, batchCreateMonth, fileId, user);
            callback = fileOpListenerMgr.postUpdate(wsInfo, fileInfo);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("detachFile failed: workspace={},batchId={},fileId={}", workspaceName,
                    batchId, fileId);
            throw e;
        }
        finally {
            unlock(fileLock, fileLockPath);
            unlock(batchLock, batchLockPath);
        }
        callback.onComplete();
    }

    private ScmLock lock(ScmLockPath lockPath) throws Exception {
        return ScmLockManager.getInstance().acquiresLock(lockPath);
    }

    private void unlock(ScmLock lock, ScmLockPath lockPath) {
        try {
            if (lock != null) {
                lock.unlock();
            }
        }
        catch (Exception e) {
            logger.error("failed to unlock:path=" + lockPath);
        }
    }

    /**
     * Whether the batch contains the specified file.
     */
    private boolean isFileInBatch(BSONObject batch, String fileId) {
        BasicBSONList files = (BasicBSONList) batch.get(FieldName.Batch.FIELD_FILES);
        if (null != files) {
            for (Object obj : files) {
                BSONObject file = (BSONObject) obj;
                String id = (String) file.get(FieldName.FIELD_CLFILE_ID);
                if (id != null && id.equals(fileId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Query the batch by ID and check if it exists.
     *
     * @param wsInfo
     * @param batchId
     * @return batch info
     * @throws ScmServerException
     */
    private BSONObject getAndCheckBatch(ScmWorkspaceInfo wsInfo, String batchId,
            String batchCreateMonth) throws ScmServerException {
        BSONObject batch = batchDao.queryById(wsInfo, batchId, batchCreateMonth);
        if (null == batch) {
            throw new ScmServerException(ScmError.BATCH_NOT_FOUND,
                    "batch is unexist: workspaceName=" + wsInfo.getName() + ", batchId=" + batchId);
        }
        return batch;
    }

    private ScmWorkspaceInfo getWorkspace(String workspaceName) throws ScmServerException {
        return ScmContentServer.getInstance().getWorkspaceInfoChecked(workspaceName);
    }

    @Override
    public long countBatch(String wsName, BSONObject condition) throws ScmServerException {
        return ScmContentServer.getInstance().getMetaService().getBatchCount(wsName, condition);
    }
}