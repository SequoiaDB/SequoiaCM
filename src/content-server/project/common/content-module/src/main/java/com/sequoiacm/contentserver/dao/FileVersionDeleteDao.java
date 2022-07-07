package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileVersionDeleteDao {
    private static final Logger logger = LoggerFactory.getLogger(FileVersionDeleteDao.class);

    private final ScmContentModule contentModule;
    private final ScmWorkspaceInfo wsInfo;
    private final String fileId;
    private final int majorVersion;
    private final int minorVersion;
    private final FileOperationListenerMgr listenerMgr;
    private final BucketInfoManager bucketInfoManager;

    public FileVersionDeleteDao(String ws, String fileId, int majorVersion, int minorVersion,
            FileOperationListenerMgr listenerMgr, BucketInfoManager bucketInfoManager)
            throws ScmServerException {
        this.contentModule = ScmContentModule.getInstance();
        this.wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(ws);
        this.fileId = fileId;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.listenerMgr = listenerMgr;
        this.bucketInfoManager = bucketInfoManager;
    }

    public FileVersionDeleteDao(ScmBucket bucket, String fileName, int majorVersion,
            int minorVersion, FileOperationListenerMgr listenerMgr,
            BucketInfoManager bucketInfoManager) throws ScmServerException {
        this.contentModule = ScmContentModule.getInstance();
        this.wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        BSONObject file;
        try {
            file = bucket.getFileTableAccessor(null).queryOne(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, fileName), null, null);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to get file info: bucket="
                    + bucket.getName() + ", fileName=" + fileName, e);
        }
        if (file == null) {
            // 文件不存在，置空，后续delete函数做空实现
            this.fileId = null;
        }
        else {
            this.fileId = BsonUtils.getStringChecked(file, FieldName.BucketFile.FILE_ID);
        }
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.listenerMgr = listenerMgr;
        this.bucketInfoManager = bucketInfoManager;
    }

    // 返回被删除的版本
    public BSONObject delete() throws ScmServerException {
        if (fileId == null) {
            return null;
        }
        BSONObject deletedVersion = null;
        TransactionContext trans = null;
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresWriteLock(ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId));
        try {
            ContentModuleMetaSource metasource = contentModule.getMetaService().getMetaSource();
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);

            BasicBSONObject fileMatcher = new BasicBSONObject();
            ScmMetaSourceHelper.addFileIdAndCreateMonth(fileMatcher, fileId);

            BSONObject latestVersion = fileAccessor.queryOne(fileMatcher, null, null);
            if (latestVersion == null) {
                logger.info(
                        "file not exist, ignore delete file version: ws={}, fileId={}, fileVersion={}.{}",
                        wsInfo.getName(), fileId, majorVersion, minorVersion);
                return null;
            }

            trans = metasource.createTransactionContext();
            trans.begin();
            MetaFileHistoryAccessor fileHistoryAccessor = metasource
                    .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), trans);
            fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                    trans);
            if (!isDeleteLatestVersion(latestVersion)) {
                // 不是删除最新版本，需要去历史表上做删除
                fileMatcher.append(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion)
                        .append(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
                deletedVersion = fileHistoryAccessor.queryAndDelete(fileMatcher, null,
                        latestVersion);
                if (deletedVersion == null) {
                    rollbackTrans(trans);
                    return null;
                }
            }
            else {
                // 删除最新版本，需要将历史表上的最新版本，覆盖至最新表上
                deletedVersion = latestVersion;
                BSONObject latestVersionInHistory = fileHistoryAccessor.queryOne(fileMatcher,
                        new BasicBSONObject(FieldName.FIELD_CLFILE_MAJOR_VERSION, -1)
                                .append(FieldName.FIELD_CLFILE_MINOR_VERSION, -1));
                if (latestVersionInHistory != null) {
                    fileHistoryAccessor.delete(fileId,
                            BsonUtils.getIntegerChecked(latestVersionInHistory,
                                    FieldName.FIELD_CLFILE_MAJOR_VERSION),
                            BsonUtils.getIntegerChecked(latestVersionInHistory,
                                    FieldName.FIELD_CLFILE_MINOR_VERSION));
                    ScmFileVersionHelper.updateLatestVersionAsNewVersion(wsInfo, fileId,
                            latestVersionInHistory, latestVersion, trans);
                }
                else {
                    // 历史表上没有记录，直接删除最新表记录
                    fileAccessor.delete(fileMatcher);
                    ScmFileOperateUtils.deleteBucketFileRelForDeleteFile(bucketInfoManager,
                            deletedVersion, trans);
                    ScmFileOperateUtils.deleteFileRelForDeleteFile(wsInfo, fileId, deletedVersion,
                            trans);
                }
            }
            trans.commit();
        }
        catch (Exception e) {
            rollbackTrans(trans);
            ScmError error = ScmError.SYSTEM_ERROR;
            if (e instanceof ScmMetasourceException) {
                error = ((ScmMetasourceException) e).getScmError();
            }
            throw new ScmServerException(error,
                    "failed to delete file version: ws=" + wsInfo.getName() + ", fileId=" + fileId
                            + ", version=" + majorVersion + "." + minorVersion,
                    e);
        }
        finally {
            closeTrans(trans);
            lock.unlock();
        }

        if (deletedVersion != null) {
            final BSONObject finalDeletedVersion = deletedVersion;
            AsyncUtils.execute(new Runnable() {
                @Override
                public void run() {
                    listenerMgr.postDeleteVersion(wsInfo, finalDeletedVersion);
                    ScmFileDataDeleterWrapper dataDeleter = new ScmFileDataDeleterWrapper(wsInfo,
                            finalDeletedVersion);
                    dataDeleter.deleteDataSilence();
                }
            });
        }
        return deletedVersion;
    }

    private void closeTrans(TransactionContext transactionContext) {
        if (transactionContext != null) {
            transactionContext.close();
        }
    }

    private void rollbackTrans(TransactionContext transactionContext) {
        if (transactionContext != null) {
            transactionContext.rollback();
        }
    }

    private boolean isDeleteLatestVersion(BSONObject latestVersion) {
        if (majorVersion == -1 && minorVersion == -1) {
            return true;
        }
        if (majorVersion == BsonUtils.getInteger(latestVersion,
                FieldName.FIELD_CLFILE_MAJOR_VERSION)
                && minorVersion == BsonUtils.getIntegerChecked(latestVersion,
                        FieldName.FIELD_CLFILE_MINOR_VERSION)) {
            return true;
        }
        return false;
    }
}
