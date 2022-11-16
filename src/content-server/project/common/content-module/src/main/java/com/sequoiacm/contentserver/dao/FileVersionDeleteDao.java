package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileVersionDeleteDao {
    private static final Logger logger = LoggerFactory.getLogger(FileVersionDeleteDao.class);

    @Autowired
    private FileOperationListenerMgr listenerMgr;
    @Autowired
    private BucketInfoManager bucketInfoManager;
    @Autowired
    private FileMetaOperator fileMetaOperator;

    public FileMeta delete(String ws, String fileId, int majorVersion, int minorVersion)
            throws ScmServerException {
        FileMeta deletedVersion = null;
        ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(ws, fileId);
        ScmLock wLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);
        try {
            deletedVersion = fileMetaOperator
                    .deleteFileVersionMeta(ws, fileId, majorVersion, minorVersion)
                    .getDeletedVersion();
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.FILE_NOT_FOUND) {
                throw e;
            }
            logger.debug(
                    "file not exist, ignore delete file version: ws={}, fileId={}, fileVersion={}.{}",
                    ws, fileId, majorVersion, minorVersion, e);
        }
        finally {
            wLock.unlock();
        }

        if (deletedVersion != null) {
            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws);
            FileMeta finalDeletedVersion = deletedVersion;
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

    public FileMeta delete(ScmBucket bucket, String fileName, int majorVersion, int minorVersion)
            throws ScmServerException {
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
            return null;
        }
        String fileId = BsonUtils.getStringChecked(file, FieldName.BucketFile.FILE_ID);
        return delete(bucket.getWorkspace(), fileId, majorVersion, minorVersion);
    }
}
