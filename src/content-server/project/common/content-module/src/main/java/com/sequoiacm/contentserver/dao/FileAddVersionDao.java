package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.pipeline.file.module.AddFileMetaVersionResult;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import org.bson.BSONObject;

import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileAddVersionDao {

    public static class Context {
        private FileMeta currentLatestVersion;
        private String fileId;

        private FileMeta newVersion;
        private String ws;
        private TransactionCallback transactionCallback;
        private boolean hasLock;

        private Context(boolean hasLock, String ws, String fileId, FileMeta newVersion,
                FileMeta currentLatestVersion, TransactionCallback transactionCallback) {
            this.currentLatestVersion = currentLatestVersion;
            this.newVersion = newVersion;
            this.hasLock = hasLock;
            this.ws = ws;
            this.transactionCallback = transactionCallback;
            this.fileId = fileId;
        }

        // FileAddVersionDao 的调用者已经持有文件锁，Dao 在处理时无需再获取锁
        public static Context lockInCaller(String ws, FileMeta newVersion, FileMeta latestVersion,
                TransactionCallback transactionCallback) {
            return new Context(true, ws, latestVersion.getId(), newVersion, latestVersion,
                    transactionCallback);
        }

        // FileAddVersionDao 在处理时会先获取文件锁，再处理文件
        public static Context standard(String ws, String fileId, FileMeta newVersion,
                TransactionCallback transactionCallback) {
            return new Context(false, ws, fileId, newVersion, null, transactionCallback);
        }
    }

    @Autowired
    private BucketInfoManager bucketMgr;
    @Autowired
    private FileOperationListenerMgr listenerMgr;

    @Autowired
    private FileMetaOperator fileMetaOperator;

    private FileInfoAndOpCompleteCallback addVersion(String wsName, String fileId,
            FileMeta newFileVersion, TransactionCallback transactionCallback)
            throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(ws.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            BSONObject latestFileVersion = ScmContentModule.getInstance().getMetaService()
                    .getFileInfo(ws.getMetaLocation(), ws.getName(), fileId, -1, -1);
            if (latestFileVersion == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file not exist: ws=" + ws.getName() + ", fileId=" + fileId);
            }
            FileMeta latestFileVersionMeta = FileMeta.fromRecord(latestFileVersion);
            return addVersionNoLock(wsName, newFileVersion, latestFileVersionMeta,
                    transactionCallback);
        }
        finally {
            writeLock.unlock();
        }
    }

    private FileInfoAndOpCompleteCallback addVersionNoLock(String wsName, FileMeta newFileVersion,
            FileMeta latestFileVersion, TransactionCallback transactionCallback)
            throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(wsName);
        listenerMgr.preAddVersion(ws, newFileVersion);
        AddFileMetaVersionResult res = fileMetaOperator.addFileMetaVersion(wsName,
                newFileVersion, latestFileVersion, transactionCallback);
        if (res.getDeletedVersion() != null) {
            listenerMgr.postDeleteVersion(ws, res.getDeletedVersion());
            AsyncUtils.execute(() -> {
                final ScmFileDataDeleterWrapper dataDeleter = new ScmFileDataDeleterWrapper(ws,
                        res.getDeletedVersion());
                dataDeleter.deleteDataSilence();
            });
        }
        OperationCompleteCallback operationCompleteCallback = listenerMgr.postAddVersion(ws,
                res.getNewVersion().getId());
        return new FileInfoAndOpCompleteCallback(newFileVersion, operationCompleteCallback);
    }

    public FileInfoAndOpCompleteCallback addVersion(Context context) throws ScmServerException {
        if (context.hasLock) {
            return addVersionNoLock(context.ws, context.newVersion, context.currentLatestVersion,
                    context.transactionCallback);
        }
        return addVersion(context.ws, context.fileId, context.newVersion,
                context.transactionCallback);
    }

}
