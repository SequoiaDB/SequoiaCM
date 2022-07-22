package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;

public class FileAddVersionDao {

    public static class Context {
        private BSONObject currentLatestVersion;
        private BSONObject newVersion;
        private boolean hasLock;

        private Context(boolean hasLock, BSONObject newVersion, BSONObject currentLatestVersion) {
            this.currentLatestVersion = currentLatestVersion;
            this.newVersion = newVersion;
            this.hasLock = hasLock;
        }

        // FileAddVersionDao 的调用者已经持有文件锁，Dao 在处理时无需再获取锁
        public static Context lockInCaller(BSONObject newVersion, BSONObject latestVersion) {
            return new Context(true, newVersion, latestVersion);
        }

        // FileAddVersionDao 在处理时会先获取文件锁，再处理文件
        public static Context standard(BSONObject newVersion) {
            return new Context(false, newVersion, null);
        }
    }

    private final ScmContentModule contentModule;
    private final ScmWorkspaceInfo ws;
    private final String fileId;
    private final BucketInfoManager bucketMgr;
    private final TransactionCallback transCallback;
    private final FileOperationListenerMgr listenerMgr;

    public FileAddVersionDao(ScmWorkspaceInfo ws, String fileId,
            TransactionCallback transactionCallback, BucketInfoManager bucketInfoManager,
            FileOperationListenerMgr listenerMgr) {
        contentModule = ScmContentModule.getInstance();
        this.ws = ws;
        this.fileId = fileId;
        this.bucketMgr = bucketInfoManager;
        this.transCallback = transactionCallback;
        this.listenerMgr = listenerMgr;
    }

    private FileInfoAndOpCompleteCallback addVersion(BSONObject newFileVersion)
            throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(ws.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            BSONObject latestFileVersion = ScmContentModule.getInstance().getMetaService()
                    .getFileInfo(ws.getMetaLocation(), ws.getName(), fileId, -1, -1);
            if (latestFileVersion == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file not exist: ws=" + ws.getName() + ", fileId=" + fileId);
            }
            return addVersionNoLock(newFileVersion, latestFileVersion);
        }
        finally {
            writeLock.unlock();
        }
    }

    private FileInfoAndOpCompleteCallback addVersionNoLock(BSONObject newFileVersion,
            BSONObject latestFileVersion)
            throws ScmServerException {
        OperationCompleteCallback operationCompleteCallback;
        BSONObject deletedVersion = null;
        TransactionContext transactionContext = null;
        try {
            ScmFileVersionHelper.resetNewFileVersion(newFileVersion, latestFileVersion);
            listenerMgr.preAddVersion(ws, newFileVersion);

            ScmBucket fileBucket = null;
            Number bucketId = BsonUtils.getNumber(newFileVersion,
                    FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
            if (bucketId != null) {
                fileBucket = bucketMgr.getBucketById(bucketId.longValue());
                if (fileBucket != null) {
                    checkFileMd5EtagExist(newFileVersion);
                }
            }

            // 文件不属于任何bucket时，按 ENABLED 情况进行处理
            ScmBucketVersionStatus versionStatus = fileBucket == null
                    ? ScmBucketVersionStatus.Enabled
                    : fileBucket.getVersionStatus();

            transactionContext = contentModule.getMetaService().getMetaSource()
                    .createTransactionContext();
            transactionContext.begin();
            if (versionStatus == ScmBucketVersionStatus.Disabled
                    || versionStatus == ScmBucketVersionStatus.Suspended) {
                // 桶版本为关闭状态或暂停状态时，新增的文件版本必须是 null 版本
                // 一个文件中只能存在一个 null 版本，需要把旧的 null 版本删除掉，再添加新的 null 版本
                // 为了减少数据库操作次数，可以将删除最新表记录 + 插入新版本记录的操作优化成更新操作
                if (!ScmFileVersionHelper.isSpecifiedVersion(latestFileVersion,
                        CommonDefine.File.NULL_VERSION_MAJOR,
                        CommonDefine.File.NULL_VERSION_MINOR)) {
                    // 最新版本不是 null version，去历史表删除 version 版本
                    deletedVersion = ScmFileVersionHelper.deleteNullVersionInHistory(ws, fileId,
                            transactionContext, latestFileVersion);
                    // 把最新版本复制到历史表
                    ScmFileVersionHelper.insertVersionToHistory(ws, latestFileVersion,
                            transactionContext);
                }
                else {
                    // 最新版本是 null version
                    deletedVersion = latestFileVersion;
                }

                newFileVersion.put(FieldName.FIELD_CLFILE_VERSION_SERIAL,
                        newFileVersion.get(FieldName.FIELD_CLFILE_MAJOR_VERSION) + "."
                                + newFileVersion.get(FieldName.FIELD_CLFILE_MINOR_VERSION));
                newFileVersion.put(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                        CommonDefine.File.NULL_VERSION_MAJOR);
                newFileVersion.put(FieldName.FIELD_CLFILE_MINOR_VERSION,
                        CommonDefine.File.NULL_VERSION_MINOR);
                // 更新操作，将新版元数据覆盖到最新表的记录上
                ScmFileVersionHelper.updateLatestVersionAsNewVersion(ws, fileId, newFileVersion,
                        latestFileVersion, transactionContext);
            }
            else if (versionStatus == ScmBucketVersionStatus.Enabled) {
                ScmFileVersionHelper.insertVersionToHistory(ws, latestFileVersion,
                        transactionContext);
                ScmFileVersionHelper.updateLatestVersionAsNewVersion(ws, fileId, newFileVersion,
                        latestFileVersion, transactionContext);
            }
            else {
                throw new ScmServerException(ScmError.SYSTEM_ERROR,
                        "unknown version status: versionStatus=" + versionStatus);
            }

            if (transCallback != null) {
                transCallback.beforeTransactionCommit(transactionContext);
            }
            transactionContext.commit();
            if (deletedVersion != null) {
                listenerMgr.postDeleteVersion(ws, deletedVersion);
            }
            operationCompleteCallback = listenerMgr.postAddVersion(ws, fileId);
        }
        catch (ScmMetasourceException e) {
            rollback(transactionContext);
            throw new ScmServerException(e.getScmError(), "failed to add new version: ws="
                    + ws.getName() + ", fileId=" + fileId + ", newVersion=" + newFileVersion, e);
        }
        catch (Exception e) {
            rollback(transactionContext);
            throw e;
        }
        finally {
            close(transactionContext);
        }
        if (deletedVersion != null) {
            final ScmFileDataDeleterWrapper dataDeleter = new ScmFileDataDeleterWrapper(ws,
                    deletedVersion);
            AsyncUtils.execute(new Runnable() {
                @Override
                public void run() {
                    dataDeleter.deleteDataSilence();
                }
            });
        }
        return new FileInfoAndOpCompleteCallback(newFileVersion, operationCompleteCallback);
    }

    private void checkFileMd5EtagExist(BSONObject newFileVersion) throws ScmServerException {
        if (BsonUtils.getBooleanOrElse(newFileVersion, FieldName.FIELD_CLFILE_DELETE_MARKER,
                false)) {
            return;
        }

        String md5 = BsonUtils.getString(newFileVersion, FieldName.FIELD_CLFILE_FILE_MD5);
        if (md5 == null || md5.isEmpty()) {
            String etag = BsonUtils.getString(newFileVersion, FieldName.FIELD_CLFILE_FILE_ETAG);
            if (etag == null || etag.isEmpty()) {
                throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                        "failed to create new version, the file version in bucket must contain md5/etag attribute : ws="
                                + ws.getName() + ", file=" + fileId);
            }
        }
    }

    public FileInfoAndOpCompleteCallback addVersion(Context context) throws ScmServerException {
        if (context.hasLock) {
            return addVersionNoLock(context.newVersion, context.currentLatestVersion);
        }
        return addVersion(context.newVersion);
    }

    private void close(TransactionContext transactionContext) {
        if (transactionContext != null) {
            transactionContext.close();
        }
    }

    private void rollback(TransactionContext transactionContext) {
        if (transactionContext != null) {
            transactionContext.rollback();
        }
    }

}
