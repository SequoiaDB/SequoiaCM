package com.sequoiacm.contentserver.quota.limiter;

import com.sequoiacm.contentserver.quota.QuotaInfo;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.msg.EnableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.QuotaMsg;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://ujyczvcfvj.feishu.cn/wiki/wikcnOi8Kow6EBLojamSHALSi8c
 * 无限额状态限额控制器，状态转换规则如下： EnableQuotaMsg => StableStatusQuotaLimiter
 *
 */
public class UnlimitedStatusQuotaLimiter implements QuotaLimiter {
    private static final Logger logger = LoggerFactory.getLogger(UnlimitedStatusQuotaLimiter.class);

    private String bucketName;
    private BucketQuotaManager quotaManager;

    private volatile QuotaWrapper quotaUsedInfo = new QuotaWrapper();

    public UnlimitedStatusQuotaLimiter(String bucketName, BucketQuotaManager quotaManager) {
        this.bucketName = bucketName;
        this.quotaManager = quotaManager;
    }

    @Override
    public QuotaInfo acquireQuota(long num, long size, long createTime) {
        return new QuotaInfo(bucketName, num, size, createTime);
    }

    @Override
    public void releaseQuota(long num, long size, long createTime) {

    }

    @Override
    public LimiterType handleMsg(QuotaMsg quotaMsg) throws ScmServerException {
        logger.info("handle msg:{}", quotaMsg);
        if (quotaMsg instanceof EnableQuotaMsg) {
            return LimiterType.STABLE;
        }
        return this.getType();
    }

    @Override
    public boolean setUsedQuota(long usedObjects, long usedSize, int quotaRoundNumber) {
        this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
        return true;
    }

    @Override
    public QuotaWrapper getQuotaUsedInfo() {
        return quotaUsedInfo;
    }

    @Override
    public int getQuotaRoundNumber() {
        return -1;
    }

    @Override
    public void destroySilence() {

    }

    @Override
    public BSONObject getInfo() {
        BSONObject info = new BasicBSONObject();
        info.put("limiter", UnlimitedStatusQuotaLimiter.class.getSimpleName());
        info.put("bucketName", bucketName);
        info.put("quotaUsedInfo", quotaUsedInfo.toBSONObject());
        return info;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public LimiterType getType() {
        return LimiterType.UNLIMITED;
    }

    @Override
    public void beforeLimiterChange(QuotaMsg quotaMsg) {

    }

    @Override
    public void afterLimiterChange(QuotaMsg quotaMsg) {

    }
}
