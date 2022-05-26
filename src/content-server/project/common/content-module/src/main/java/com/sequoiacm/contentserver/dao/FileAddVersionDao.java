package com.sequoiacm.contentserver.dao;

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
                if (!ScmFileVersionHelper.fileVersionHasNullMarker(latestFileVersion)) {
                    // 最新版本不含 null marker，去历史表删除带 null marker 的记录
                    deletedVersion = ScmFileVersionHelper.deleteNullMarkerInHistory(ws, fileId,
                            transactionContext, latestFileVersion);
                    // 把最新版本复制到历史表
                    ScmFileVersionHelper.insertVersionToHistory(ws, latestFileVersion,
                            transactionContext);
                }
                else {
                    // 最新版本含 null marker，标记下
                    deletedVersion = latestFileVersion;
                }
                newFileVersion.put(FieldName.FIELD_CLFILE_NULL_MARKER, true);
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
