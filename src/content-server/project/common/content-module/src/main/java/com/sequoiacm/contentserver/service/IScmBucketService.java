package com.sequoiacm.contentserver.service;

import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.model.ObjectDeltaInfo;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.model.SessionInfoWrapper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface IScmBucketService {
    ScmBucket createBucket(ScmUser user, String ws, String name) throws ScmServerException;

    long countFile(ScmUser user, String bucketName, Integer scope, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException;

    ScmBucket getBucket(ScmUser user, String name) throws ScmServerException;

    ScmBucket getBucket(ScmUser user, long id) throws ScmServerException;

    ScmBucket getBucket(String name) throws ScmServerException;

    ScmBucket getBucket(long id) throws ScmServerException;

    ScmBucket deleteBucket(ScmUser user, String name) throws ScmServerException;

    ScmObjectCursor<ScmBucket> listBucket(ScmUser user, BSONObject condition, BSONObject orderBy,
            long skip, long limit) throws ScmServerException;

    long countBucket(ScmUser user, BSONObject condition) throws ScmServerException;

    FileMeta createFile(ScmUser user, String bucket, FileMeta fileInfo, InputStream data,
            boolean isOverWrite) throws ScmServerException;

    FileMeta createFile(ScmUser user, String bucket, FileMeta fileInfo, String breakpointFile,
            boolean isOverWrite) throws ScmServerException;

    FileMeta createFile(ScmUser user, String bucket, FileMeta fileInfo,
            TransactionCallback transactionCallback, boolean isOverWrite) throws ScmServerException;

    BSONObject getFileVersion(ScmUser user, String bucket, String fileName, int majorVersion,
            int minorVersion) throws ScmServerException;

    BSONObject getFileVersion(String bucket, String fileName, int majorVersion,
                              int minorVersion) throws ScmServerException;

    BSONObject getFileNullVersion(ScmUser user, String bucket, String fileName)
            throws ScmServerException;

    MetaCursor listFile(ScmUser user, String bucketName, Integer scope, BSONObject condition,
            BSONObject selector, BSONObject orderBy, long skip, long limit,
            boolean isResContainsDeleteMarker) throws ScmServerException;

    String getFileId(ScmUser user, String bucket, String fileName) throws ScmServerException;

    List<ScmBucketAttachFailure> attachFile(ScmUser user, String bucket, List<String> fileIdList,
            ScmBucketAttachKeyType type) throws ScmServerException;

    void detachFile(ScmUser user, String bucket, String fileName) throws ScmServerException;

    ScmBucket updateBucketVersionStatus(ScmUser user, String bucketName,
            ScmBucketVersionStatus bucketVersionStatus) throws ScmServerException;

    // 删除文件若发生了新增版本（deleteMarker），则返回这个新增的版本，否则返回null
    FileMeta deleteFile(ScmUser user, String bucket, String fileName, boolean isPhysical,
            SessionInfoWrapper sessionInfoWrapper) throws ScmServerException;

    // 返回被删除的版本
    FileMeta deleteFileVersion(ScmUser user, String bucket, String fileName, int majorVersion,
            int minorVersion) throws ScmServerException;

    FileMeta deleteNullVersionFile(ScmUser user, String bucket, String fileName)
            throws ScmServerException;

    void setBucketTag(ScmUser user, String bucketName, Map<String, String> customTag)
            throws ScmServerException;

    Map<String, String> getBucketTag(ScmUser user, String bucketName) throws ScmServerException;

    void deleteBucketTag(ScmUser user, String bucketName) throws ScmServerException;

    ObjectDeltaInfo getObjectDelta(String bucketName, BSONObject condition)
            throws ScmServerException;

    long getAllBucketFileCount(ScmWorkspaceInfo wsInfo, ScmBucket bucket, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException, ScmMetasourceException;

    MetaCursor queryAllBucketFile(ScmWorkspaceInfo wsInfo, ScmBucket bucket, BSONObject matcher,
            BSONObject selector, BSONObject orderBy, boolean isResContainsDeleteMarker)
            throws ScmServerException, ScmMetasourceException;
}
