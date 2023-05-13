package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.contentserver.quota.QuotaLimiterIncorrectException;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.exception.ScmServerException;

public interface WriteTablePolicy {

    void acquireQuota(long acquireObjects, long acquireSize, long maxObjects, long maxSize,
            long createTime) throws ScmServerException;

    void releaseQuota(long num, long size, long createTime) throws ScmServerException;

    void flush(boolean force) throws QuotaLimiterIncorrectException;

    String getName();

    long getUsedObjectsCache();

    long getUsedSizeCache();

    QuotaWrapper getQuotaUsedInfo();

    void setUsedQuota(long usedObjects, long usedSize);
}
