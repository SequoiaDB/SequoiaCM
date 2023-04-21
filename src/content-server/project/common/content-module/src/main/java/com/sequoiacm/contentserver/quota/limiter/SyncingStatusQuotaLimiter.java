package com.sequoiacm.contentserver.quota.limiter;

import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.quota.QuotaInfo;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaRequestRecord;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.msg.BeginSyncMsg;
import com.sequoiacm.contentserver.quota.msg.CancelSyncMsg;
import com.sequoiacm.contentserver.quota.msg.DisableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.FinishSyncMsg;
import com.sequoiacm.contentserver.quota.msg.QuotaMsg;
import com.sequoiacm.contentserver.quota.msg.QuotaSyncMsg;
import com.sequoiacm.contentserver.quota.msg.SetAgreementTimeMsg;
import com.sequoiacm.contentserver.quota.msg.SyncExpiredMsg;
import com.sequoiacm.contentserver.quota.msg.SyncFailedMsg;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * https://ujyczvcfvj.feishu.cn/wiki/wikcnOi8Kow6EBLojamSHALSi8c
 * 同步状态限额控制器，状态转换规则如下： DisableQuotaMsg => UnlimitedStatusQuotaLimiter
 * FinishSyncMsg、CancelSyncMsg、SyncFailedMsg、SyncExpiredMsg =>
 * StableStatusQuotaLimiter
 *
 */
public class SyncingStatusQuotaLimiter implements QuotaLimiter {

    private static final Logger logger = LoggerFactory.getLogger(SyncingStatusQuotaLimiter.class);
    private static final int STATISTICS_QUOTA_UPDATE_INTERVAL = 3 * 1000;

    private int syncRoundNumber;
    private String bucketName;
    private volatile Long agreementTime;
    private BucketQuotaManager quotaManager;
    private int quotaRoundNumber;

    private volatile QuotaWrapper quotaUsedInfo = new QuotaWrapper();

    // 在协商出 agreementTime 之前，存放在这里面
    private final Map<Long, QuotaRequestRecord> requestRecords = new ConcurrentHashMap<>();

    private volatile QuotaWrapper afterAgreementTimeQuotaUsedInfo = new QuotaWrapper();

    private volatile QuotaWrapper totalQuotaCache = new QuotaWrapper();
    private volatile QuotaWrapper lastStatisticsQuotaInfo;
    private volatile long lastStatisticsQuotaUpdateTime = System.currentTimeMillis();

    private ScmTimer exitTimer = ScmTimerFactory.createScmTimer();

    private ReadWriteLock agreementTimeLock = new ReentrantReadWriteLock();

    private boolean isFlushed = false;

    public SyncingStatusQuotaLimiter(String bucketName, BucketQuotaManager quotaManager,
            int syncRoundNumber, long expireTime, long usedObjects, long usedSize,
            int quotaRoundNumber) {
        init(bucketName, quotaManager, syncRoundNumber, expireTime, usedObjects, usedSize,
                quotaRoundNumber);
    }

    public SyncingStatusQuotaLimiter(QuotaMsg quotaMsg, BucketQuotaManager quotaManager,
            QuotaWrapper quotaUsedInfo, int quotaRoundNumber) throws ScmSystemException {
        if (quotaMsg instanceof BeginSyncMsg) {
            BeginSyncMsg beginSyncMsg = (BeginSyncMsg) quotaMsg;
            init(quotaMsg.getName(), quotaManager, beginSyncMsg.getSyncRoundNumber(),
                    beginSyncMsg.getExpireTime(), quotaUsedInfo.getObjects(),
                    quotaUsedInfo.getSize(), quotaRoundNumber);
        }
        else {
            throw new ScmSystemException("unsupported quota msg: " + quotaMsg);
        }
    }

    private void init(String bucketName, BucketQuotaManager quotaManager, int syncRoundNumber,
            long expireTime, long usedObjects, long usedSize, int quotaRoundNumber) {
        this.bucketName = bucketName;
        this.quotaManager = quotaManager;
        this.syncRoundNumber = syncRoundNumber;
        this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
        this.quotaRoundNumber = quotaRoundNumber;
        exitTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                if (agreementTime == null) {
                    try {
                        quotaManager.handleMsg(new SyncExpiredMsg(BucketQuotaManager.QUOTA_TYPE,
                                bucketName, syncRoundNumber));
                        cancel();
                    }
                    catch (ScmServerException e) {
                        logger.error("handle sync expired msg failed, bucketName: " + bucketName,
                                e);
                    }
                }
                else {
                    cancel();
                }
            }
        }, expireTime, expireTime);
    }

    @Override
    public QuotaInfo acquireQuota(long acquireObjects, long acquireSize, long createTime)
            throws ScmServerException {
        QuotaWrapper statisticsQuotaInfo = getStatisticsQuotaInfo();
        long maxObjects = quotaManager.getBucketMaxObjects(bucketName);
        if (maxObjects >= 0 && acquireObjects > 0) {
            // 这里的计算不一定准确，totalQuotaCache 可能会包含 statisticsQuotaInfo 里面一部分的额度信息
            long remainObjects = maxObjects - totalQuotaCache.getObjects()
                    - statisticsQuotaInfo.getObjects();
            if (remainObjects < acquireObjects) {
                logger.warn(
                        "bucket object count exceeded, bucketName={},acquireObjects={},remainObjects={}",
                        bucketName, acquireObjects, remainObjects);
                throw new ScmServerException(ScmError.BUCKET_QUOTA_EXCEEDED,
                        "bucket quota exceeded: bucketName=" + bucketName + ", maxObjects="
                                + maxObjects);
            }
        }
        long maxSize = quotaManager.getBucketMaxSize(bucketName);
        if (maxSize >= 0 && acquireSize > 0) {
            long remainSize = maxSize - totalQuotaCache.getSize() - statisticsQuotaInfo.getSize();
            if (remainSize < acquireSize) {
                logger.warn(
                        "bucket object size  exceeded, bucketName={},acquireSize={},remainSize={}",
                        bucketName, acquireSize, remainSize);
                throw new ScmServerException(ScmError.BUCKET_QUOTA_EXCEEDED,
                        "bucket quota exceeded: bucketName: " + bucketName + ", maxSize: "
                                + ScmQuotaUtils.formatSize(maxSize));

            }
        }
        recordQuota(acquireObjects, acquireSize, createTime);
        return new QuotaInfo(bucketName, acquireObjects, acquireSize, createTime);
    }

    private QuotaWrapper getStatisticsQuotaInfo() throws ScmSystemException {
        // 内部缓存额度统计信息，每三秒更新一次
        if (lastStatisticsQuotaInfo == null) {
            synchronized (this) {
                if (lastStatisticsQuotaInfo == null) {
                    lastStatisticsQuotaInfo = quotaManager.getStatisticsQuotaInfo(bucketName,
                            syncRoundNumber);
                    lastStatisticsQuotaUpdateTime = System.currentTimeMillis();
                    return lastStatisticsQuotaInfo;
                }
            }
        }

        if ((System.currentTimeMillis() - lastStatisticsQuotaUpdateTime) > STATISTICS_QUOTA_UPDATE_INTERVAL) {
            synchronized (this) {
                if ((System.currentTimeMillis() - lastStatisticsQuotaUpdateTime) > STATISTICS_QUOTA_UPDATE_INTERVAL) {
                    lastStatisticsQuotaInfo = quotaManager.getStatisticsQuotaInfo(bucketName,
                            syncRoundNumber);
                    lastStatisticsQuotaUpdateTime = System.currentTimeMillis();
                    return lastStatisticsQuotaInfo;
                }
            }
        }
        return lastStatisticsQuotaInfo;
    }

    @Override
    public void releaseQuota(long num, long size, long createTime) {
        recordQuota(-num, -size, createTime);
    }

    @Override
    public LimiterType handleMsg(QuotaMsg quotaMsg) throws ScmServerException {
        logger.info("handle msg:{}", quotaMsg);
        if (quotaMsg instanceof BeginSyncMsg) {
            BeginSyncMsg beginSyncMsg = (BeginSyncMsg) quotaMsg;
            if (beginSyncMsg.getSyncRoundNumber() > this.getSyncRoundNumber()) {
                logger.warn(
                        "bucket {} sync round number {} is greater than current round number {}",
                        getBucketName(), beginSyncMsg.getSyncRoundNumber(), getSyncRoundNumber());
                return LimiterType.NONE;
            }
        }
        if (quotaMsg instanceof QuotaSyncMsg) {
            QuotaSyncMsg syncMsg = (QuotaSyncMsg) quotaMsg;
            if (syncMsg.getSyncRoundNumber() < getSyncRoundNumber()) {
                logger.warn("bucket {} sync round number {} is less than current round number {}",
                        getBucketName(), syncMsg.getSyncRoundNumber(), getSyncRoundNumber());
                return LimiterType.NONE;
            }
        }
        if (quotaMsg instanceof DisableQuotaMsg) {
            return LimiterType.UNLIMITED;
        }
        if (quotaMsg instanceof FinishSyncMsg || quotaMsg instanceof CancelSyncMsg
                || quotaMsg instanceof SyncFailedMsg) {
            return LimiterType.STABLE;
        }
        if (quotaMsg instanceof SetAgreementTimeMsg) {
            setAgreementTime(((SetAgreementTimeMsg) quotaMsg).getAgreementTime());
        }
        if (quotaMsg instanceof SyncExpiredMsg) {
            if (getAgreementTime() != null) {
                return this.getType();
            }
            return LimiterType.STABLE;
        }
        return this.getType();
    }

    @Override
    public boolean setUsedQuota(long usedObjects, long usedSize, int quotaRoundNumber) {
        if (quotaRoundNumber != this.quotaRoundNumber) {
            logger.warn("bucket {} quota round number {} is not equal to current quota round number {}",
                    getBucketName(), quotaRoundNumber, this.quotaRoundNumber);
            return false;
        }
        this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
        return true;
    }

    @Override
    public QuotaWrapper getQuotaUsedInfo() {
        return quotaUsedInfo;
    }

    @Override
    public int getQuotaRoundNumber() {
        return quotaRoundNumber;
    }

    @Override
    public LimiterType getType() {
        return LimiterType.SYNCING;
    }

    @Override
    public void beforeLimiterChange(QuotaMsg quotaMsg) {

    }

    @Override
    public void afterLimiterChange(QuotaMsg quotaMsg) {
        if (quotaMsg instanceof FinishSyncMsg) {
            isFlushed = true;
            flushAfterAgreementTimeQuotaCacheSilence();
        }
        if (quotaMsg instanceof CancelSyncMsg || quotaMsg instanceof SyncFailedMsg
                || quotaMsg instanceof SyncExpiredMsg) {
            isFlushed = true;
            flushAllQuotaCacheSilence();
        }
    }

    private void recordQuota(long objects, long size, long createTime) {
        if (objects == 0 && size == 0) {
            return;
        }
        totalQuotaCache.addObjects(objects);
        totalQuotaCache.addSize(size);
        Lock lock = agreementTimeLock.readLock();
        try {
            lock.lock();
            long seconds = createTime / 1000;
            if (agreementTime == null) {
                QuotaRequestRecord quotaRequestRecord = requestRecords.get(seconds);
                if (quotaRequestRecord != null) {
                    quotaRequestRecord.addUsedObjects(objects);
                    quotaRequestRecord.addUsedSize(size);
                }
                else {
                    synchronized (requestRecords) {
                        quotaRequestRecord = requestRecords.get(seconds);
                        if (quotaRequestRecord != null) {
                            quotaRequestRecord.addUsedObjects(objects);
                            quotaRequestRecord.addUsedSize(size);
                        }
                        else {
                            quotaRequestRecord = new QuotaRequestRecord(bucketName, objects, size,
                                    seconds);
                            requestRecords.put(seconds, quotaRequestRecord);
                        }
                    }
                }
                return;
            }

            if (seconds > agreementTime / 1000) {
                afterAgreementTimeQuotaUsedInfo.addObjects(objects);
                afterAgreementTimeQuotaUsedInfo.addSize(size);
            }
        }
        finally {
            lock.unlock();
        }

    }

    @Override
    public void destroySilence() {
        if (exitTimer != null) {
            exitTimer.cancel();
        }
        if (!isFlushed) {
            flushAllQuotaCacheSilence();
        }
    }

    @Override
    public BSONObject getInfo() {
        BSONObject info = new BasicBSONObject();
        info.put("limiter", SyncingStatusQuotaLimiter.class.getSimpleName());
        info.put("bucketName", bucketName);
        info.put("syncRoundNumber", syncRoundNumber);
        info.put("agreementTime", agreementTime);
        info.put("quotaUsedInfo", quotaUsedInfo.toBSONObject());
        info.put("totalQuotaCache", totalQuotaCache.toBSONObject());
        info.put("afterAgreementTimeQuotaUsedInfo", afterAgreementTimeQuotaUsedInfo.toBSONObject());
        return info;
    }

    public int getSyncRoundNumber() {
        return syncRoundNumber;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    public Long getAgreementTime() {
        return agreementTime;
    }

    public BucketQuotaManager getQuotaManager() {
        return quotaManager;
    }

    public void setAgreementTime(Long agreementTime) {
        Lock lock = agreementTimeLock.writeLock();
        try {
            lock.lock();
            this.agreementTime = agreementTime;
            long agreementTimeSeconds = agreementTime / 1000;
            for (Map.Entry<Long, QuotaRequestRecord> entry : requestRecords.entrySet()) {
                if (entry.getKey() > agreementTimeSeconds) {
                    afterAgreementTimeQuotaUsedInfo.addObjects(entry.getValue().getObjects());
                    afterAgreementTimeQuotaUsedInfo.addSize(entry.getValue().getSize());
                }
            }
            requestRecords.clear();
        }
        finally {
            lock.unlock();
        }

    }

    private void flushAfterAgreementTimeQuotaCacheSilence() {
        try {
            QuotaWrapper old = afterAgreementTimeQuotaUsedInfo;
            afterAgreementTimeQuotaUsedInfo = new QuotaWrapper();
            quotaUsedInfo = quotaManager.addUsedInfoToQuotaTable(bucketName, quotaRoundNumber,
                    old.getObjects(), old.getSize());
        }
        catch (Exception e) {
            logger.error("failed to flush quota cache, bucketName: " + bucketName, e);
        }
    }

    private void flushAllQuotaCacheSilence() {
        try {
            QuotaWrapper old = totalQuotaCache;
            totalQuotaCache = new QuotaWrapper();
            quotaUsedInfo = quotaManager.addUsedInfoToQuotaTable(bucketName, quotaRoundNumber,
                    old.getObjects(), old.getSize());
        }
        catch (Exception e) {
            logger.error("failed to flush quota cache, bucketName: " + bucketName, e);
        }
    }
}
