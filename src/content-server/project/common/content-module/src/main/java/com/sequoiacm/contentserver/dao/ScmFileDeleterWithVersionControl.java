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
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
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

    public ScmFileDeleterWithVersionControl(String sessionId, String userDetail, String userName,
            ScmBucket bucket, String fileName, FileOperationListenerMgr listenerMgr,
            BucketInfoManager bucketInfoMgr) throws ScmServerException {
        this.fileName = fileName;
        this.bucket = bucket;
        this.listenerMgr = listenerMgr;
        this.bucketInfoMgr = bucketInfoMgr;
        this.contentModule = ScmContentModule.getInstance();
        this.wsInfo = contentModule.getWorkspaceInfoCheckExist(bucket.getWorkspace());
        this.sessionId = sessionId;
        this.userDetail = userDetail;
        this.userName = userName;
    }

    @Override
    public BSONObject delete() throws ScmServerException {
        FileInfoAndOpCompleteCallback fileInfoAndOpCompleteCallback;
        ScmLock wLock = null;
        try {
            BSONObject file = bucket.getFileTableAccessor(null).queryOne(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, fileName), null, null);
            if (file == null) {
                return createDeleteMarkerFile();
            }
            String fileId = BsonUtils.getStringChecked(file, FieldName.BucketFile.FILE_ID);

            ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(),
                    fileId);
            wLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);

            BSONObject latestVersionInLock = contentModule.getMetaService()
                    .getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, -1, -1);
            if (null == latestVersionInLock) {
                return createDeleteMarkerFile();
            }

            ScmBucket fileBucketInLock = null;
            Number bucketId = BsonUtils.getNumber(latestVersionInLock,
                    FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
            if (bucketId != null) {
                fileBucketInLock = bucketInfoMgr.getBucketById(bucketId.longValue());
            }

            if (fileBucketInLock == null || !fileBucketInLock.getName().equals(bucket.getName())) {
                // 查询到文件ID后，加锁之前，这个文件又被重新关联到其它桶上了，所以这里会出现锁内文件的所属桶非指定的桶
                // 所以这里认为指定桶下已经不存在这个文件了，直接创建 deleteMarker
                return createDeleteMarkerFile();
            }

            if (fileBucketInLock.getVersionStatus() == ScmBucketVersionStatus.Disabled) {
                ScmFileDeletorPysical fileDeleterPhysical = new ScmFileDeletorPysical(sessionId,
                        userDetail, wsInfo, fileId, listenerMgr, bucketInfoMgr);
                return fileDeleterPhysical.deleteNoLock();
            }

            BSONObject newFileVersion = createDeleteMarkerBSON();
            ScmFileVersionHelper.resetNewFileVersion(newFileVersion, latestVersionInLock);

            FileAddVersionDao addVersionDao = new FileAddVersionDao(wsInfo, fileId, null,
                    bucketInfoMgr, listenerMgr);
            fileInfoAndOpCompleteCallback = addVersionDao
                    .addVersion(FileAddVersionDao.Context.lockInCaller(newFileVersion,
                            latestVersionInLock));
        }
        catch (FileExistWhenCreateDeleteMarker retryException) {
            logger.debug(
                    "failed to create delete marker file cause by file exist, try delete again: bucket={}, fileName={}",
                    bucket.getName(), fileName, retryException);
            return delete();
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            ScmError error = ScmError.SYSTEM_ERROR;
            if (e instanceof ScmMetasourceException) {
                error = ((ScmMetasourceException) e).getScmError();
            }
            throw new ScmServerException(error,
                    "failed to delete file with version control: bucket=" + bucket.getName()
                            + ", fileName=" + fileName,
                    e);
        }
        finally {
            if (wLock != null) {
                wLock.unlock();
            }
        }
        fileInfoAndOpCompleteCallback.getCallback().onComplete();
        return fileInfoAndOpCompleteCallback.getFileInfo();

    }

    private BSONObject createDeleteMarkerBSON() throws ScmServerException {
        BSONObject deleteMarkerBson = new BasicBSONObject();
        deleteMarkerBson.put(FieldName.FIELD_CLFILE_NAME, fileName);

        Date date = new Date();
        String fileId = ScmIdGenerator.FileId.get(date);
        deleteMarkerBson = ScmFileOperateUtils.formatFileObj(wsInfo, deleteMarkerBson, fileId, date,
                userName);
        deleteMarkerBson.put(FieldName.FIELD_CLFILE_DELETE_MARKER, true);
        deleteMarkerBson.put(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, bucket.getId());
        return deleteMarkerBson;
    }

    private BSONObject createDeleteMarkerFile()
            throws ScmMetasourceException, ScmServerException, FileExistWhenCreateDeleteMarker {
        if (bucket.getVersionStatus() == ScmBucketVersionStatus.Disabled) {
            return null;
        }

        BSONObject deleteMarkerFile = createDeleteMarkerBSON();
        BSONObject bucketFileRel = ScmFileOperateUtils.createBucketFileRel(deleteMarkerFile);
        ContentModuleMetaSource metasource = contentModule.getMetaService().getMetaSource();
        TransactionContext trans = metasource.createTransactionContext();
        try {
            MetaAccessor bucketFileTableAccessor = bucket.getFileTableAccessor(trans);
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), trans);
            trans.begin();
            try {
                bucketFileTableAccessor.insert(bucketFileRel);
            }
            catch (ScmMetasourceException e) {
                if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                    throw new FileExistWhenCreateDeleteMarker(e);
                }
                throw e;
            }

            try {
                fileAccessor.insert(deleteMarkerFile);
            }
            catch (ScmMetasourceException e) {
                trans.rollback();
                if (e.getScmError() == ScmError.FILE_TABLE_NOT_FOUND) {
                    logger.debug("create table", e);
                    try {
                        metasource.getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null)
                                .createFileTable(deleteMarkerFile);
                    }
                    catch (Exception ex) {
                        throw new ScmServerException(ScmError.METASOURCE_ERROR,
                                "insert file failed, create file table failed:ws="
                                        + wsInfo.getName() + ", file=" + deleteMarkerFile,
                                ex);
                    }
                    return createDeleteMarkerFile();
                }
                throw e;
            }
            trans.commit();
        }
        catch (Exception e) {
            trans.rollback();
            throw e;
        }
        finally {
            trans.close();
        }

        return deleteMarkerFile;
    }

    private static class FileExistWhenCreateDeleteMarker extends Exception {
        private FileExistWhenCreateDeleteMarker(Throwable cause) {
            super(cause);
        }
    }
}
