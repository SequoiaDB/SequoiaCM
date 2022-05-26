package com.sequoiacm.s3.service;

import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ListBucketResult;
import com.sequoiacm.s3.model.LocationConstraint;
import com.sequoiacm.s3.model.VersioningConfigurationBase;

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
}
