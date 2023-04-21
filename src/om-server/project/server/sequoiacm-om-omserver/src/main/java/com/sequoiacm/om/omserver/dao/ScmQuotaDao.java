package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.om.omserver.exception.ScmInternalException;

public interface ScmQuotaDao {
    void enableBucketQuota(String bucketName, Long maxObjects, String maxSize)
            throws ScmInternalException;

    void updateBucketQuota(String bucketName, Long maxObjects, String maxSize)
            throws ScmInternalException;

    void disableBucketQuota(String bucketName) throws ScmInternalException;

    void syncBucketQuota(String bucketName) throws ScmInternalException;

    void cancelSyncBucketQuota(String bucketName) throws ScmInternalException;

    ScmBucketQuotaInfo getBucketQuota(String bucketName) throws ScmInternalException;
}
