package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaExistException;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class ScmFileDeleterWithVersionControl implements ScmFileDeletor {
    private static final Logger logger = LoggerFactory
            .getLogger(ScmFileDeleterWithVersionControl.class);
    private final FileOperationListenerMgr listenerMgr;
    private final BucketInfoManager bucketInfoMgr;
    private final ScmContentModule contentModule;
    private final String fileName;
    private final ScmBucket bucket;
    private final ScmWorkspaceInfo wsInfo;
    private final String userDetail;
    private final String sessionId;
    private final String userName;
    private final FileMetaOperator fileMetaOperator;
    private final FileAddVersionDao addVersionDao;

    public ScmFileDeleterWithVersionControl(String sessionId, String userDetail, String userName,
            ScmBucket bucket, String fileName, FileOperationListenerMgr listenerMgr,
            BucketInfoManager bucketInfoMgr, FileMetaOperator fileMetaOperator,
            FileAddVersionDao addVersionDao) throws ScmServerException {
        this.fileName = fileName;
        this.bucket = bucket;
        this.listenerMgr = listenerMgr;
        this.bucketInfoMgr = bucketInfoMgr;
        this.contentModule = ScmContentModule.getInstance();
        this.wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        this.sessionId = sessionId;
        this.userDetail = userDetail;
        this.userName = userName;
        this.fileMetaOperator = fileMetaOperator;
        this.addVersionDao = addVersionDao;
    }

    private String queryFileId() throws ScmServerException {
        try {
            BSONObject file = bucket.getFileTableAccessor(null).queryOne(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, fileName), null, null);
            if (file == null) {
                return null;
            }
            return BsonUtils.getStringChecked(file, FieldName.BucketFile.FILE_ID);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to get file info: bucket="
                    + bucket.getName() + ", fileName=" + fileName, e);
        }
    }

    private boolean isFileInBucket(BSONObject fileInfo, String expectFileInBucket)
            throws ScmServerException {
        Number bucketId = BsonUtils.getNumber(fileInfo, FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        ScmBucket actualFileBucket = null;
        if (bucketId != null) {
            actualFileBucket = bucketInfoMgr.getBucketById(bucketId.longValue());
        }

        if (actualFileBucket == null || !actualFileBucket.getName().equals(expectFileInBucket)) {
            return false;
        }
        return true;
    }

    @Override
    public FileMeta delete() throws ScmServerException {
        if (bucket.getVersionStatus() == ScmBucketVersionStatus.Disabled) {
            // disabled 状态走物理删除
            if (contentModule.getLocalSiteInfo().isRootSite()) {
                checkAndPhysicallyDelete();
                return null;
            }
            // 将本次版本控制删除转发给主站点，由主站点进行处理（这里不能直接发一个物理删除给主站点，需要让主站点按版本控制删除流程再次锁内检查文件，再做物理删除）
            return forwardToMainSite();
        }
        String fileId = queryFileId();
        if (fileId == null) {
            try {
                return createDeleteMarkerFile();

            }
            catch (FileMetaExistException e) {
                logger.debug(
                        "failed to create delete marker file cause by file exist, try add delete marker version: bucket={}, fileName={}",
                        bucket.getName(), fileName, e);
                return addDeleteMarkerVersion(e.getExistFileId());
            }
        }
        return addDeleteMarkerVersion(fileId);
    }

    private FileMeta addDeleteMarkerVersion(String fileId) throws ScmServerException {
        FileInfoAndOpCompleteCallback fileInfoAndOpCompleteCallback;
        ScmLock wLock = null;
        try {
            ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            wLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);

            BSONObject latestVersionInLock = contentModule.getMetaService()
                    .getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, -1, -1);
            if (null == latestVersionInLock) {
                return createDeleteMarkerFile();
            }

            if (!isFileInBucket(latestVersionInLock, bucket.getName())) {
                // 查询到文件ID后，加锁之前，这个文件又被重新关联到其它桶上了，所以这里会出现锁内文件的所属桶非指定的桶
                // 所以这里认为指定桶下已经不存在这个文件了，直接创建 deleteMarker
                return createDeleteMarkerFile();
            }
            FileMeta newFileVersion = FileMeta.deleteMarkerMeta(wsInfo.getName(), fileName,
                    userName, bucket.getId());
            fileInfoAndOpCompleteCallback = addVersionDao
                    .addVersion(FileAddVersionDao.Context.lockInCaller(wsInfo.getName(),
                            newFileVersion, FileMeta.fromRecord(latestVersionInLock), null));
        }
        catch (FileMetaExistException e) {
            logger.debug(
                    "failed to create delete marker file cause by file exist, try add delete marker version: bucket={}, fileName={}",
                    bucket.getName(), fileName, e);
            if (wLock != null) {
                wLock.unlock();
                wLock = null;
            }
            return addDeleteMarkerVersion(e.getExistFileId());
        }
        finally {
            if (wLock != null) {
                wLock.unlock();
            }
        }

        fileInfoAndOpCompleteCallback.getCallback().onComplete();
        return fileInfoAndOpCompleteCallback.getFileInfo();
    }

    private void checkAndPhysicallyDelete() throws ScmServerException {
        ScmLock wLock = null;
        try {
            String fileId = queryFileId();
            if (fileId == null) {
                return;
            }
            ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            wLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);

            BSONObject latestVersionInLock = contentModule.getMetaService()
                    .getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, -1, -1);
            if (latestVersionInLock == null
                    || !isFileInBucket(latestVersionInLock, bucket.getName())) {
                return;
            }
            ScmFileDeletorPysical fileDeleterPhysical = new ScmFileDeletorPysical(sessionId,
                    userDetail, wsInfo, fileId, listenerMgr, fileMetaOperator);
            fileDeleterPhysical.deleteInMainSiteNoLock();
        }
        finally {
            if (wLock != null) {
                wLock.unlock();
            }
        }
    }

    private FileMeta forwardToMainSite() throws ScmServerException {
        String remoteSiteName = contentModule.getMainSiteName();
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByServiceName(remoteSiteName);
        return FileMeta.fromRecord(client.deleteFileInBucket(sessionId, userDetail,
                bucket.getName(), fileName, false));
    }


    private FileMeta createDeleteMarkerFile() throws ScmServerException {
        FileMeta fileMeta = FileMeta.deleteMarkerMeta(wsInfo.getName(), fileName, userName,
                bucket.getId());
        return fileMetaOperator.createFileMeta(wsInfo.getName(), fileMeta, null).getNewFile();
    }

    private static class FileExistWhenCreateDeleteMarker extends Exception {
        private FileExistWhenCreateDeleteMarker(Throwable cause) {
            super(cause);
        }
    }
}
