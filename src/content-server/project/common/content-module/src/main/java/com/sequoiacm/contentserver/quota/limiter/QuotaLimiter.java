package com.sequoiacm.contentserver.quota.limiter;

import com.sequoiacm.contentserver.quota.QuotaInfo;
import com.sequoiacm.contentserver.quota.QuotaLimiterIncorrectException;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.msg.QuotaMsg;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

public interface QuotaLimiter {

    QuotaInfo acquireQuota(long num, long size, long createTime) throws ScmServerException;

    void releaseQuota(long num, long size, long createTime) throws ScmServerException;

    /**
     * 接收一个消息，返回一个的 LimiterType，如果 LimiterType 发生变化，QuotaManager 会根据返回的 LimiterType
     * 创建新的 QuotaLimiter， 如果返回 LimiterType.NONE，则会移除当前的 QuotaLimiter，不会创建新的
     */
    LimiterType handleMsg(QuotaMsg quotaMsg) throws ScmServerException;

    void setUsedQuota(long usedObjects, long usedSize, int quotaRoundNumber)
            throws QuotaLimiterIncorrectException;

    QuotaWrapper getQuotaUsedInfo();

    int getQuotaRoundNumber();

    void destroySilence();

    BSONObject getInfo();

    String getBucketName();

    LimiterType getType();

    void beforeLimiterChange(QuotaMsg quotaMsg);

    void afterLimiterChange(QuotaMsg quotaMsg);
}
