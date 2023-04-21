package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.om.omserver.common.QuotaLevel;
import com.sequoiacm.om.omserver.dao.ScmQuotaDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.factory.ScmQuotaDaoFactory;
import com.sequoiacm.om.omserver.module.OmBucketQuotaInfo;
import com.sequoiacm.om.omserver.service.ScmQuotaService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ScmQuotaServiceImpl implements ScmQuotaService {

    @Autowired
    private ScmQuotaDaoFactory quotaDaoFactory;

    @Override
    public void enableBucketQuota(ScmOmSession session, String name, Long maxObjects,
            String maxSize) throws ScmInternalException {
        if (maxObjects == null && maxSize == null) {
            throw new IllegalArgumentException(
                    "maxSize and maxObjects can not be null at the same time");
        }
        ScmQuotaDao quotaDao = quotaDaoFactory.createQuotaDao(session);
        quotaDao.enableBucketQuota(name, maxObjects, maxSize);
    }

    @Override
    public void updateBucketQuota(ScmOmSession session, String bucketName, Long maxObjects,
            String maxSize) throws ScmInternalException {
        if (maxObjects == null && maxSize == null) {
            throw new IllegalArgumentException(
                    "maxSize and maxObjects can not be null at the same time");
        }
        ScmQuotaDao quotaDao = quotaDaoFactory.createQuotaDao(session);
        quotaDao.updateBucketQuota(bucketName, maxObjects, maxSize);
    }

    @Override
    public void disableBucketQuota(ScmOmSession session, String bucketName)
            throws ScmInternalException {
        quotaDaoFactory.createQuotaDao(session).disableBucketQuota(bucketName);
    }

    @Override
    public void syncBucketQuota(ScmOmSession session, String bucketName)
            throws ScmInternalException {
        quotaDaoFactory.createQuotaDao(session).syncBucketQuota(bucketName);
    }

    @Override
    public void cancelSyncBucketQuota(ScmOmSession session, String name)
            throws ScmInternalException {
        quotaDaoFactory.createQuotaDao(session).cancelSyncBucketQuota(name);
    }

    @Override
    public OmBucketQuotaInfo getBucketQuota(ScmOmSession session, String bucketName)
            throws ScmInternalException {
        ScmBucketQuotaInfo quotaInfo = quotaDaoFactory.createQuotaDao(session)
                .getBucketQuota(bucketName);
        return transferQuotaInfo(quotaInfo);
    }

    private OmBucketQuotaInfo transferQuotaInfo(ScmBucketQuotaInfo quotaInfo) {
        OmBucketQuotaInfo result = new OmBucketQuotaInfo();
        result.setBucketName(quotaInfo.getBucketName());
        result.setMaxObjects(quotaInfo.getMaxObjects());
        result.setMaxSizeBytes(quotaInfo.getMaxSizeBytes());
        result.setUsedObjects(quotaInfo.getUsedObjects());
        result.setUsedSizeBytes(quotaInfo.getUsedSizeBytes());
        result.setSyncStatus(
                quotaInfo.getSyncStatus() != null ? quotaInfo.getSyncStatus().getName() : null);
        result.setEnable(quotaInfo.isEnable());
        result.setErrorMsg(quotaInfo.getErrorMsg());
        result.setLastUpdateTime(
                quotaInfo.getLastUpdateTime() != null ? quotaInfo.getLastUpdateTime().getTime()
                        : null);
        result.setEstimatedEffectiveTime(quotaInfo.getEstimatedEffectiveTime());
        if (!quotaInfo.isEnable()
                || (quotaInfo.getMaxObjects() < 0 && quotaInfo.getMaxSizeBytes() < 0)) {
            result.setQuotaLevel(QuotaLevel.LOW.getName());
        }
        else {
            result.setQuotaLevel(QuotaLevel
                    .getMatchedQuotaLevel(quotaInfo.getMaxSizeBytes(), quotaInfo.getUsedSizeBytes(),
                            quotaInfo.getMaxObjects(), quotaInfo.getUsedObjects())
                    .getName());
        }
        return result;
    }
}
