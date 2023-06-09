package com.sequoiacm.cloud.adminserver.service;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.QuotaResult;
import org.springframework.security.core.Authentication;

public interface QuotaService {
    QuotaResult updateQuota(String type, String name, long maxObjects, long maxSize,
            Authentication auth) throws StatisticsException;

    QuotaResult enableQuota(String type, String name, long maxObjects, long maxSize,
            Long usedObjects, Long usedSize, Authentication auth) throws StatisticsException;

    void disableQuota(String type, String name, Authentication auth) throws StatisticsException;

    QuotaResult getQuota(String type, String name, boolean isForceRefresh, Authentication auth)
            throws StatisticsException;

    QuotaResult updateQuotaUsedInfo(String type, String name, Long usedObjects, Long usedSize,
            Authentication auth) throws StatisticsException;
}
