package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.limiter.QuotaLimiter;
import com.sequoiacm.exception.ScmServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HighWaterLevelStrategy extends AbstractWaterLevelStrategy {
    private static final Logger logger = LoggerFactory.getLogger(HighWaterLevelStrategy.class);

    public HighWaterLevelStrategy(String bucketName, int quotaRoundNumber, long usedObjects,
            long usedSize, QuotaLimitConfig quotaLimitConfig, QuotaLimiter quotaLimiter,
            BucketQuotaManager quotaManager) throws ScmServerException {
        super(bucketName, quotaRoundNumber, usedObjects, usedSize, quotaLimitConfig, quotaLimiter,
                quotaManager);
        logger.info("init HighWaterLevelStrategy: bucket={},quotaRoundNumber={}", bucketName,
                quotaRoundNumber);
    }

    @Override
    protected WriteTablePolicy createWriteTablePolicy(long usedObjects, long usedSize)
            throws ScmServerException {
        if (quotaLimitConfig.getHighWater().getPolicy().equals(QuotaLimitConfig.POLICY_SYNC)) {
            return new SyncWriteTablePolicy(bucketName, quotaRoundNumber, usedObjects, usedSize);
        }
        else if (quotaLimitConfig.getHighWater().getPolicy()
                .equals(QuotaLimitConfig.POLICY_ASYNC)) {
            return new AsyncWriteTablePolicy(bucketName, quotaRoundNumber, usedObjects, usedSize,
                    quotaLimitConfig, quotaLimiter, quotaManager);
        }
        else {
            throw new IllegalArgumentException(
                    "unknown policy: " + quotaLimitConfig.getHighWater().getPolicy());
        }
    }

    @Override
    public WaterLevel getWaterLevel() {
        return WaterLevel.HIGH_WATER;
    }

    @Override
    public void refreshWriteTablePolicy() throws ScmServerException {
        if (!writeTablePolicy.getName().equals(quotaLimitConfig.getHighWater().getPolicy())) {
            WriteTablePolicy old = this.writeTablePolicy;
            QuotaWrapper quotaUsedInfo = old.getQuotaUsedInfo();
            this.writeTablePolicy = createWriteTablePolicy(quotaUsedInfo.getObjects(),
                    quotaUsedInfo.getSize());
            old.flush(true);
        }
    }
}
