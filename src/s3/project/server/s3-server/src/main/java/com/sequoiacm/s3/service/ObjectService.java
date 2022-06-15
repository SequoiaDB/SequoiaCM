package com.sequoiacm.s3.service;

import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.core.CopyObjectRequest;
import com.sequoiacm.s3.core.Range;
import com.sequoiacm.s3.core.S3ObjectMeta;
import com.sequoiacm.s3.core.S3PutObjectRequest;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.CopyObjectResult;
import com.sequoiacm.s3.model.DeleteObjectResult;
import com.sequoiacm.s3.model.DeleteObjects;
import com.sequoiacm.s3.model.DeleteObjectsResult;
import com.sequoiacm.s3.model.GetObjectResult;
import com.sequoiacm.s3.model.ListObjectsResult;
import com.sequoiacm.s3.model.ListObjectsResultV1;
import com.sequoiacm.s3.model.ListVersionsResult;
import com.sequoiacm.s3.model.ObjectMatcher;
import com.sequoiacm.s3.model.PutObjectResult;

public interface ObjectService {
    PutObjectResult putObject(ScmSession session, S3PutObjectRequest obj) throws S3ServerException;

    S3ObjectMeta getObjectMeta(ScmSession session, String bucketName, String objectName,
            String versionId, ObjectMatcher matchers) throws S3ServerException;

    GetObjectResult getObject(ScmSession session, String bucketName, String objectName,
            String versionId, ObjectMatcher matchers, Range range) throws S3ServerException;

    CopyObjectResult copyObject(ScmSession session, CopyObjectRequest request)
            throws S3ServerException;

    DeleteObjectResult deleteObject(ScmSession session, String bucketName, String objectName) throws S3ServerException;

    DeleteObjectResult deleteObject(ScmSession session, String bucketName, String objectName,
                                    String versionId) throws S3ServerException;

    DeleteObjectsResult deleteObjects(ScmSession session, String bucketName,
            DeleteObjects deleteObjects) throws S3ServerException;

    ListObjectsResultV1 listObjectsV1(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, int maxKeys, String encodingType)
            throws S3ServerException;

    ListVersionsResult listVersions(ScmSession session, String bucketName, String prefix,
            String delimiter, String keyMarker, String versionIdMarker, int maxKeys,
            String encodingType) throws S3ServerException;

    ListObjectsResult listObjectsV2(ScmSession session, String bucketName, String prefix,
            String delimiter, String startAfter, int maxKeys, String continueToken,
            String encodingType, boolean fetchOwner) throws S3ServerException;
}
