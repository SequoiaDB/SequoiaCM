package com.sequoiacm.s3.service;

import java.io.InputStream;

import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.core.ObjectMeta;
import com.sequoiacm.s3.core.Range;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.CopyObjectResult;
import com.sequoiacm.s3.model.GetResult;
import com.sequoiacm.s3.model.ListObjectsResult;
import com.sequoiacm.s3.model.ListObjectsResultV1;
import com.sequoiacm.s3.model.ListVersionsResult;
import com.sequoiacm.s3.model.ObjectMatcher;
import com.sequoiacm.s3.model.ObjectUri;
import com.sequoiacm.s3.model.PutDeleteResult;

public interface ObjectService {
    PutDeleteResult putObject(ScmSession session, ObjectMeta meta, InputStream inputStream)
            throws S3ServerException;

    public ObjectMeta getObjectMeta(ScmSession session, String bucketName, String objectName,
            Long versionId, boolean isNoVersion, ObjectMatcher matchers) throws S3ServerException;

    GetResult getObject(ScmSession session, String bucketName, String objectName, Long versionId,
            boolean isNoVersion, ObjectMatcher matchers, Range range) throws S3ServerException;

    CopyObjectResult copyObject(ScmSession session, ObjectMeta dest, ObjectUri sourceUri,
            ObjectMatcher matcher, boolean directiveCopy) throws S3ServerException;

    PutDeleteResult deleteObject(ScmSession session, String bucketName, String objectName)
            throws S3ServerException;

    PutDeleteResult deleteObject(ScmSession session, String bucketName, String objectName,
            Long versionId, boolean isNoVersion) throws S3ServerException;

    ListObjectsResultV1 listObjectsV1(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, Integer maxKeys, String encodingType)
            throws S3ServerException;

    ListVersionsResult listVersions(ScmSession session, String bucketName, String prefix,
            String delimiter, String keyMarker, String versionIdMarker, Integer maxKeys,
            String encodingType) throws S3ServerException;

    boolean isEmptyBucket(ScmSession session, Bucket bucket) throws S3ServerException;

    ListObjectsResult listObjectsV2(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, Integer maxKeys, String continueToken,
            String encodingType, boolean fetchOwner) throws S3ServerException;
}
