package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmBucketQuotaInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmQuotaService {

    void enableBucketQuota(ScmOmSession session, String bucketName, Long maxObjects,
            String maxSize) throws ScmInternalException;

    void updateBucketQuota(ScmOmSession session, String bucketName, Long maxObjects,
            String maxSize) throws ScmInternalException;

    void disableBucketQuota(ScmOmSession session, String bucketName) throws ScmInternalException;

    void syncBucketQuota(ScmOmSession session, String bucketName) throws ScmInternalException;

    void cancelSyncBucketQuota(ScmOmSession session, String bucketName)
            throws ScmInternalException;

    OmBucketQuotaInfo getBucketQuota(ScmOmSession session, String bucketName)
            throws ScmInternalException;
}
