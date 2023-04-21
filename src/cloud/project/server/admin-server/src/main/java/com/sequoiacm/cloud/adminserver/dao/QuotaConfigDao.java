package com.sequoiacm.cloud.adminserver.dao;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.QuotaConfigDetail;

public interface QuotaConfigDao {

    QuotaConfigDetail getQuotaConfigInfo(String type, String name) throws StatisticsException;

    void updateQuotaUsedInfo(String type, String name, Long usedObjects, Long usedSizeBytes)
            throws ScmMetasourceException;
}
