package com.sequoiacm.contentserver.service;

import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.model.SessionInfoWrapper;
import com.sequoiacm.contentserver.model.OverwriteOption;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;

import java.io.InputStream;
import java.util.List;

public interface IScmBucketService {
    ScmBucket createBucket(ScmUser user, String ws, String name) throws ScmServerException;

    long countFile(ScmUser user, String bucketName, BSONObject condition) throws ScmServerException;

    ScmBucket getBucket(ScmUser user, String name) throws ScmServerException;

    ScmBucket getBucket(ScmUser user, long id) throws ScmServerException;

    ScmBucket getBucket(String name) throws ScmServerException;

    ScmBucket getBucket(long id) throws ScmServerException;

    ScmBucket deleteBucket(ScmUser user, String name) throws ScmServerException;

    ScmObjectCursor<ScmBucket> listBucket(ScmUser user, BSONObject condition, BSONObject orderBy,
            long skip, long limit) throws ScmServerException;

    long countBucket(ScmUser user, BSONObject condition) throws ScmServerException;

    BSONObject createFile(ScmUser user, String bucket, BSONObject fileInfo, InputStream data,
                          OverwriteOption overwriteOption) throws ScmServerException;

    BSONObject createFile(ScmUser user, String bucket, BSONObject fileInfo, String breakpointFile,
            OverwriteOption overwriteOption) throws ScmServerException;

    BSONObject createFile(ScmUser user, String bucket, BSONObject fileInfo,
                          ScmDataInfoDetail data, TransactionCallback transactionCallback,
                          OverwriteOption overwriteOption) throws ScmServerException;

    BSONObject getFileVersion(ScmUser user, String bucket, String fileName, int majorVersion,
            int minorVersion) throws ScmServerException;

    BSONObject getFileVersion(String bucket, String fileName, int majorVersion,
                              int minorVersion) throws ScmServerException;

    BSONObject getFileNullVersion(ScmUser user, String bucket, String fileName)
            throws ScmServerException;

    MetaCursor listFile(ScmUser user, String bucketName, BSONObject condition, BSONObject selector,
            BSONObject orderBy, long skip, long limit) throws ScmServerException;

    String getFileId(ScmUser user, String bucket, String fileName) throws ScmServerException;

    List<ScmBucketAttachFailure> attachFile(ScmUser user, String bucket, List<String> fileIdList,
            ScmBucketAttachKeyType type) throws ScmServerException;

    void detachFile(ScmUser user, String bucket, String fileName) throws ScmServerException;

    ScmBucket updateBucketVersionStatus(ScmUser user, String bucketName,
            ScmBucketVersionStatus bucketVersionStatus) throws ScmServerException;

    // 删除文件若发生了新增版本（deleteMarker），则返回这个新增的版本，否则返回null
    BSONObject deleteFile(ScmUser user, String bucket, String fileName, boolean isPhysical,
            SessionInfoWrapper sessionInfoWrapper) throws ScmServerException;

    // 返回被删除的版本
    BSONObject deleteFileVersion(ScmUser user, String bucket, String fileName, int majorVersion,
            int minorVersion) throws ScmServerException;

    BSONObject deleteNullVersionFile(ScmUser user, String bucket, String fileName)
            throws ScmServerException;
}
