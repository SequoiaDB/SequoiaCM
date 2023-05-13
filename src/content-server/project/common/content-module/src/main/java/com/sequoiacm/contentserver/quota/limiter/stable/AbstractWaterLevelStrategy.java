package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaLimiterIncorrectException;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.limiter.QuotaLimiter;
import com.sequoiacm.exception.ScmServerException;

public abstract class AbstractWaterLevelStrategy implements WaterLevelStrategy {

    protected WriteTablePolicy writeTablePolicy;
    protected String bucketName;
    protected int quotaRoundNumber;
    protected QuotaLimitConfig quotaLimitConfig;
    protected QuotaLimiter quotaLimiter;
    protected BucketQuotaManager quotaManager;

    public AbstractWaterLevelStrategy(String bucketName, int quotaRoundNumber, long usedObjects,
            long usedSize, QuotaLimitConfig quotaLimitConfig, QuotaLimiter quotaLimiter,
            BucketQuotaManager quotaManager) throws ScmServerException {
        this.bucketName = bucketName;
        this.quotaRoundNumber = quotaRoundNumber;
        this.quotaLimitConfig = quotaLimitConfig;
        this.quotaLimiter = quotaLimiter;
        this.quotaManager = quotaManager;
        this.writeTablePolicy = createWriteTablePolicy(usedObjects, usedSize);
    }

    protected abstract WriteTablePolicy createWriteTablePolicy(long usedObjects, long usedSize)
            throws ScmServerException;

    @Override
    public WaterLevel acquireQuota(long acquireObjects, long acquireSize, long maxObjects,
            long maxSize, long createTime) throws ScmServerException {
        writeTablePolicy.acquireQuota(acquireObjects, acquireSize, maxObjects, maxSize, createTime);
        return determineWaterLevel(writeTablePolicy.getUsedObjectsCache(),
                writeTablePolicy.getUsedSizeCache(), maxObjects, maxSize,
                writeTablePolicy.getQuotaUsedInfo(), quotaLimitConfig);
    }

    @Override
    public WaterLevel releaseQuota(long acquireObjects, long acquireSize, long maxObjects,
            long maxSize, long createTime) throws ScmServerException {
        writeTablePolicy.releaseQuota(acquireObjects, acquireSize, createTime);
        return determineWaterLevel(writeTablePolicy.getUsedObjectsCache(),
                writeTablePolicy.getUsedSizeCache(), maxObjects, maxSize,
                writeTablePolicy.getQuotaUsedInfo(), quotaLimitConfig);
    }

    @Override
    public void flush(boolean force) throws QuotaLimiterIncorrectException {
        writeTablePolicy.flush(force);
    }

    @Override
    public WriteTablePolicy getWriteTablePolicy() {
        return writeTablePolicy;
    }

    public static WaterLevel determineWaterLevel(long cachedObjects, long cacheSizes,
            long maxObjects, long maxSize, QuotaWrapper quotaUsedInfo,
            QuotaLimitConfig quotaLimitConfig) {

        long usedObjects = quotaUsedInfo.getObjects() + cachedObjects;
        long usedSize = quotaUsedInfo.getSize() + cacheSizes;

        if (maxObjects >= 0) { // 小于0表示不限制
            long remainObjects = maxObjects - usedObjects;
            if (remainObjects < quotaLimitConfig.getLowWater().getMinObjects()) {
                return WaterLevel.HIGH_WATER;
            }
            if (maxObjects != 0 && usedObjects
                    / (float) maxObjects > quotaLimitConfig.getLowWater().getRate() / (float) 100) {
                return WaterLevel.HIGH_WATER;
            }
        }

        if (maxSize >= 0) {
            long remainSize = maxSize - usedSize;
            if (remainSize < quotaLimitConfig.getLowWater().getMinSizeBytes()) {
                return WaterLevel.HIGH_WATER;
            }
            if (maxSize != 0 && usedSize
                    / (float) maxSize > quotaLimitConfig.getLowWater().getRate() / (float) 100) {
                return WaterLevel.HIGH_WATER;
            }
        }
        return WaterLevel.LOW_WATER;
    }

    public static WaterLevel determineWaterLevel(long maxObjects, long maxSize,
            QuotaWrapper quotaUsedInfo, QuotaLimitConfig quotaLimitConfig) {
        return determineWaterLevel(0, 0, maxObjects, maxSize, quotaUsedInfo, quotaLimitConfig);
    }

    @Override
    public void setUsedQuota(long usedObjects, long usedSize) {
        writeTablePolicy.setUsedQuota(usedObjects, usedSize);
    }

    @Override
    public QuotaWrapper getQuotaUsedInfo() {
        return writeTablePolicy.getQuotaUsedInfo();
    }
}