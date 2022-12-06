package com.sequoiacm.s3.service;

import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ListBucketResult;
import com.sequoiacm.s3.model.LocationConstraint;
import com.sequoiacm.s3.model.VersioningConfigurationBase;

import java.util.Map;

public interface BucketService {
    void createBucket(ScmSession session, String bucketName, String region)
            throws S3ServerException;

    void deleteBucket(ScmSession session, String bucketName) throws S3ServerException;

    ListBucketResult listBucket(ScmSession session) throws S3ServerException;

    Bucket getBucket(ScmSession session, String bucketName) throws S3ServerException;

    LocationConstraint getBucketLocation(ScmSession session, String bucketName)
            throws S3ServerException;

    void setBucketVersionStatus(ScmSession session, String bucketName, String status)
            throws S3ServerException;

    VersioningConfigurationBase getBucketVersionStatus(ScmSession session, String bucketName)
            throws S3ServerException;

    void setBucketTag(ScmSession session, String bucketName, Map<String, String> bucketTag)
            throws S3ServerException;

    Map<String, String> getBucketTag(ScmSession session, String bucketName)
            throws S3ServerException;

    void deleteBucketTag(ScmSession session, String bucketName) throws S3ServerException;
}
