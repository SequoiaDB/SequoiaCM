package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.limiter.QuotaLimiter;
import com.sequoiacm.exception.ScmServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LowWaterLevelStrategy extends AbstractWaterLevelStrategy {
    private static final Logger logger = LoggerFactory.getLogger(LowWaterLevelStrategy.class);

    public LowWaterLevelStrategy(String bucketName, int quotaRoundNumber, long usedObjects,
            long usedSize, QuotaLimitConfig quotaLimitConfig, QuotaLimiter quotaLimiter,
            BucketQuotaManager quotaManager) throws ScmServerException {
        super(bucketName, quotaRoundNumber, usedObjects, usedSize, quotaLimitConfig, quotaLimiter,
                quotaManager);
        logger.info("init LowWaterLevelStrategy: bucket={},quotaRoundNumber={}", bucketName,
                quotaRoundNumber);
    }

    protected WriteTablePolicy createWriteTablePolicy(long usedObjects, long usedSize)
            throws ScmServerException {
        if (quotaLimitConfig.getLowWater().getPolicy().equals(QuotaLimitConfig.POLICY_SYNC)) {
            return new SyncWriteTablePolicy(bucketName, quotaRoundNumber, usedObjects, usedSize);
        }
        else if (quotaLimitConfig.getLowWater().getPolicy().equals(QuotaLimitConfig.POLICY_ASYNC)) {
            return new AsyncWriteTablePolicy(bucketName, quotaRoundNumber, usedObjects, usedSize,
                    quotaLimitConfig, quotaLimiter, quotaManager);
        }
        else {
            throw new IllegalArgumentException(
                    "unknown policy: " + quotaLimitConfig.getLowWater().getPolicy());
        }
    }

    @Override
    public WaterLevel getWaterLevel() {
        return WaterLevel.LOW_WATER;
    }


    @Override
    public void refreshWriteTablePolicy() throws ScmServerException {
        if (!writeTablePolicy.getName().equals(quotaLimitConfig.getLowWater().getPolicy())) {
            WriteTablePolicy old = this.writeTablePolicy;
            QuotaWrapper quotaUsedInfo = old.getQuotaUsedInfo();
            this.writeTablePolicy = createWriteTablePolicy(quotaUsedInfo.getObjects(),
                    quotaUsedInfo.getSize());
            old.flush(true);
        }
    }

}
