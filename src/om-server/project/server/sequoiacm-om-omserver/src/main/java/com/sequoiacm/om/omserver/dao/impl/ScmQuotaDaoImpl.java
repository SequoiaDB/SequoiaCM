package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.client.element.quota.ScmUpdateBucketQuotaConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmQuotaDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public class ScmQuotaDaoImpl implements ScmQuotaDao {

    private ScmOmSession session;

    public ScmQuotaDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public void enableBucketQuota(String bucketName, Long maxObjects, String maxSize)
            throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmEnableBucketQuotaConfig.Builder builder = ScmEnableBucketQuotaConfig
                    .newBuilder(bucketName);
            if (maxObjects != null) {
                builder.setMaxObjects(maxObjects);
            }
            if (maxSize != null) {
                builder.setMaxSize(maxSize);
            }
            ScmFactory.Quota.enableBucketQuota(connection, builder.build());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to enable bucket quota, " + e.getMessage(), e);
        }
    }

    @Override
    public void updateBucketQuota(String bucketName, Long maxObjects, String maxSize)
            throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmUpdateBucketQuotaConfig.Builder builder = ScmUpdateBucketQuotaConfig
                    .newBuilder(bucketName);
            if (maxObjects != null) {
                builder.setMaxObjects(maxObjects);
            }
            if (maxSize != null) {
                builder.setMaxSize(maxSize);
            }
            ScmFactory.Quota.updateBucketQuota(connection, builder.build());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to update bucket quota, " + e.getMessage(), e);
        }
    }

    @Override
    public void disableBucketQuota(String bucketName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmFactory.Quota.disableBucketQuota(connection, bucketName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to disable bucket quota, " + e.getMessage(), e);
        }
    }

    @Override
    public void syncBucketQuota(String bucketName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmFactory.Quota.syncBucketQuota(connection, bucketName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to sync bucket quota, " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelSyncBucketQuota(String bucketName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmFactory.Quota.cancelSyncBucketQuota(connection, bucketName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to cancel sync bucket quota, " + e.getMessage(), e);
        }
    }

    @Override
    public ScmBucketQuotaInfo getBucketQuota(String bucketName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            return ScmFactory.Quota.getBucketQuota(connection, bucketName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get bucket quota, " + e.getMessage(), e);
        }
    }
}
