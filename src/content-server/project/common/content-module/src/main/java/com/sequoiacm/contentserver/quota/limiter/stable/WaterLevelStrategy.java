package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.contentserver.quota.QuotaLimiterIncorrectException;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.exception.ScmServerException;

public interface WaterLevelStrategy {

    WaterLevel acquireQuota(long acquireObjects, long acquireSize, long maxObjects, long maxSize,
            long createTime) throws ScmServerException;

    WaterLevel releaseQuota(long acquireObjects, long acquireSize, long maxObjects, long maxSize,
            long createTime) throws ScmServerException;

    void flush(boolean force) throws QuotaLimiterIncorrectException;

    WaterLevel getWaterLevel();

    WriteTablePolicy getWriteTablePolicy();

    void refreshWriteTablePolicy() throws ScmServerException;

    void setUsedQuota(long usedObjects, long usedSize);

    QuotaWrapper getQuotaUsedInfo();
}
