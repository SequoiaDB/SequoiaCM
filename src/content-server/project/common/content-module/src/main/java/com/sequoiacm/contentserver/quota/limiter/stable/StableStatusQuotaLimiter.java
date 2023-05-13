package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.quota.QuotaLimiterIncorrectException;
import com.sequoiacm.contentserver.quota.ScmRefreshScopeRefreshedListener;
import com.sequoiacm.contentserver.quota.QuotaCacheAutoFlushable;
import com.sequoiacm.contentserver.quota.QuotaInfo;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.limiter.LimiterType;
import com.sequoiacm.contentserver.quota.limiter.QuotaLimiter;
import com.sequoiacm.contentserver.quota.msg.BeginSyncMsg;
import com.sequoiacm.contentserver.quota.msg.CancelSyncMsg;
import com.sequoiacm.contentserver.quota.msg.DisableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.EnableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.FinishSyncMsg;
import com.sequoiacm.contentserver.quota.msg.QuotaMsg;
import com.sequoiacm.contentserver.quota.msg.SyncExpiredMsg;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * https://ujyczvcfvj.feishu.cn/wiki/wikcnOi8Kow6EBLojamSHALSi8c
 * 稳定状态限额控制器，状态转换规则如下： DisableQuotaMsg => UnlimitedStatusQuotaLimiter
 * BeginSyncMsg => SyncingStatusQuotaLimiter
 * 
 */
public class StableStatusQuotaLimiter
        implements QuotaLimiter, QuotaCacheAutoFlushable, ScmRefreshScopeRefreshedListener {
    private static final Logger logger = LoggerFactory.getLogger(StableStatusQuotaLimiter.class);

    private ReadWriteLock waterLevelChangeLock = new ReentrantReadWriteLock();
    private QuotaLimitConfig quotaLimitConfig;

    private String bucketName;

    private int quotaRoundNumber;

    private BucketQuotaManager quotaManager;

    private WaterLevelStrategy currentWaterLevelStrategy;

    public StableStatusQuotaLimiter(String bucketName, BucketQuotaManager quotaManager,
            long usedObjects, long usedSize, int quotaRoundNumber) throws ScmServerException {
        init(bucketName, quotaManager, usedObjects, usedSize, quotaRoundNumber);
    }

    public StableStatusQuotaLimiter(QuotaMsg quotaMsg, BucketQuotaManager quotaManager,
            QuotaWrapper usedInfo) throws ScmServerException {
        boolean msgSupported = quotaMsg instanceof EnableQuotaMsg
                || quotaMsg instanceof FinishSyncMsg || quotaMsg instanceof CancelSyncMsg
                || quotaMsg instanceof SyncExpiredMsg;
        if (!msgSupported) {
            throw new ScmSystemException("unsupported quota msg: " + quotaMsg);
        }
        init(quotaMsg.getName(), quotaManager, usedInfo.getObjects(), usedInfo.getSize(),
                quotaMsg.getQuotaRoundNumber());
    }

    private void init(String bucketName, BucketQuotaManager quotaManager, long usedObjects,
            long usedSize, int quotaRoundNumber) throws ScmServerException {
        this.bucketName = bucketName;
        this.quotaManager = quotaManager;
        this.quotaLimitConfig = quotaManager.getQuotaLimitConfig();
        this.quotaRoundNumber = quotaRoundNumber;
        initWaterLevel(usedObjects, usedSize);
    }

    @Override
    public void onRefreshScopeRefreshed() throws ScmServerException {
        currentWaterLevelStrategy.refreshWriteTablePolicy();
    }

    private void initWaterLevel(long usedObjects, long usedSize) throws ScmServerException {
        QuotaConfig quotaConfig = quotaManager.getQuotaConfig(bucketName, quotaRoundNumber);
        if (quotaConfig == null) {
            throw new QuotaLimiterIncorrectException(this);
        }
        WaterLevel waterLevel = AbstractWaterLevelStrategy.determineWaterLevel(
                quotaConfig.getMaxObjects(), quotaConfig.getMaxSize(),
                new QuotaWrapper(usedObjects, usedSize), quotaLimitConfig);
        this.currentWaterLevelStrategy = createQuotaStrategy(waterLevel, usedObjects, usedSize);
    }

    private WaterLevelStrategy createQuotaStrategy(WaterLevel waterLevel, long usedObjects,
            long usedSize) throws ScmServerException {
        if (waterLevel == WaterLevel.LOW_WATER) {
            return new LowWaterLevelStrategy(bucketName, quotaRoundNumber, usedObjects, usedSize,
                    quotaLimitConfig, this, quotaManager);
        }
        else if (waterLevel == WaterLevel.HIGH_WATER) {
            return new HighWaterLevelStrategy(bucketName, quotaRoundNumber, usedObjects, usedSize,
                    quotaLimitConfig, this, quotaManager);
        }
        else {
            throw new IllegalArgumentException("unknown water level: " + waterLevel);
        }
    }

    @Override
    public QuotaInfo acquireQuota(long num, long size, long createTime) throws ScmServerException {
        QuotaConfig quotaConfig = quotaManager.getQuotaConfig(bucketName, quotaRoundNumber);
        if (quotaConfig == null) {
            throw new QuotaLimiterIncorrectException(this);
        }
        Lock readLock = waterLevelChangeLock.readLock();
        try {
            readLock.lock();
            WaterLevel newWaterLevel = currentWaterLevelStrategy.acquireQuota(num, size,
                    quotaConfig.getMaxObjects(), quotaConfig.getMaxSize(), createTime);
            if (newWaterLevel != currentWaterLevelStrategy.getWaterLevel()) {
                readLock.unlock();
                readLock = null;
                switchWaterLevel(newWaterLevel);
            }
            return new QuotaInfo(bucketName, num, size, createTime);
        }
        finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }
    }

    @Override
    public void releaseQuota(long num, long size, long createTime) throws ScmServerException {
        QuotaConfig quotaConfig = quotaManager.getQuotaConfig(bucketName, quotaRoundNumber);
        if (quotaConfig == null) {
            throw new QuotaLimiterIncorrectException(this);
        }
        Lock readLock = waterLevelChangeLock.readLock();
        try {
            readLock.lock();
            WaterLevel newWaterLevel = currentWaterLevelStrategy.releaseQuota(num, size,
                    quotaConfig.getMaxObjects(), quotaConfig.getMaxSize(), createTime);
            if (newWaterLevel != currentWaterLevelStrategy.getWaterLevel()) {
                readLock.unlock();
                readLock = null;
                switchWaterLevel(newWaterLevel);
            }
        }
        finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }
    }

    private void switchWaterLevel(WaterLevel newWaterLevel) throws ScmServerException {
        Lock writeLock = waterLevelChangeLock.writeLock();
        try {
            writeLock.lock();
            if (newWaterLevel == this.currentWaterLevelStrategy.getWaterLevel()) {
                return;
            }
            logger.info("switch water level: bucketName={},oldWaterLevel={},newWaterLevel={}",
                    bucketName, currentWaterLevelStrategy.getWaterLevel(), newWaterLevel);
            currentWaterLevelStrategy.flush(true);
            QuotaWrapper quotaUsedInfo = currentWaterLevelStrategy.getQuotaUsedInfo();
            currentWaterLevelStrategy = createQuotaStrategy(newWaterLevel,
                    quotaUsedInfo.getObjects(), quotaUsedInfo.getSize());
        }
        finally {
            writeLock.unlock();
        }

    }

    @Override
    public LimiterType handleMsg(QuotaMsg quotaMsg) {
        logger.info("handle msg:{}", quotaMsg);
        if (quotaRoundNumber < quotaMsg.getQuotaRoundNumber()) {
            logger.warn("bucket {} quota round number {} is less than current quota number {}",
                    bucketName, quotaRoundNumber, quotaMsg.getQuotaRoundNumber());
            return LimiterType.NONE;
        }

        if (quotaMsg instanceof DisableQuotaMsg) {
            return LimiterType.UNLIMITED;
        }
        if (quotaMsg instanceof BeginSyncMsg) {
            return LimiterType.SYNCING;
        }
        return this.getType();
    }

    @Override
    public void setUsedQuota(long usedObjects, long usedSize, int quotaRoundNumber)
            throws QuotaLimiterIncorrectException {
        if (this.quotaRoundNumber != quotaRoundNumber) {
            logger.warn(
                    "bucket {} quota round number {} is not equal to current quota round number {}",
                    bucketName, quotaRoundNumber, this.quotaRoundNumber);
            throw new QuotaLimiterIncorrectException(StableStatusQuotaLimiter.this);
        }
        this.currentWaterLevelStrategy.setUsedQuota(usedObjects, usedSize);
    }

    @Override
    public QuotaWrapper getQuotaUsedInfo() {
        return currentWaterLevelStrategy.getQuotaUsedInfo();
    }

    @Override
    public int getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    @Override
    public void destroySilence() {
        try {
            flushCache(true);
        }
        catch (Exception e) {
            logger.warn("failed to flush cache:bucket={},quotaRoundNumber={}", bucketName,
                    quotaRoundNumber, e);
        }
    }

    @Override
    public BSONObject getInfo() {
        BSONObject info = new BasicBSONObject();
        info.put("limiter", StableStatusQuotaLimiter.class.getSimpleName());
        info.put("bucketName", bucketName);
        info.put("quotaRoundNumber", quotaRoundNumber);
        info.put("waterLevel", currentWaterLevelStrategy.getWaterLevel());
        info.put("writeTablePolicy", currentWaterLevelStrategy.getWriteTablePolicy().getName());
        info.put("quotaUsedInfo", getQuotaUsedInfo());
        return info;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public LimiterType getType() {
        return LimiterType.STABLE;
    }

    @Override
    public void beforeLimiterChange(QuotaMsg quotaMsg) {

    }

    @Override
    public void afterLimiterChange(QuotaMsg quotaMsg) {

    }

    @Override
    public void flushCache(boolean force) throws QuotaLimiterIncorrectException {
        currentWaterLevelStrategy.flush(force);
    }

}
