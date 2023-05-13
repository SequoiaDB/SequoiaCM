package com.sequoiacm.contentserver.quota.limiter.stable;

import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaLimiterIncorrectException;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.limiter.QuotaHelper;
import com.sequoiacm.contentserver.quota.limiter.QuotaLimiter;
import com.sequoiacm.exception.ScmServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncWriteTablePolicy implements WriteTablePolicy {
    private static final Logger logger = LoggerFactory.getLogger(AsyncWriteTablePolicy.class);

    private String bucketName;
    private int quotaRoundNumber;
    private volatile QuotaWrapper quotaUsedInfo; // 已使用额度，周期从额度表中同步
    private volatile QuotaWrapper quotaDeltaInfo = new QuotaWrapper(); // 本地额度增量（缓存在本地还未写表的一部分额度）
    private QuotaLimitConfig quotaLimitConfig;
    private BucketQuotaManager quotaManager;
    private QuotaLimiter quotaLimiter;

    private long lastFlushTime = System.currentTimeMillis();
    private boolean isNeedFlush = false;
    private final Lock quotaLock = new ReentrantLock();

    public AsyncWriteTablePolicy(String bucketName, int quotaRoundNumber, long usedObjects,
            long usedSize, QuotaLimitConfig quotaLimitConfig, QuotaLimiter quotaLimiter,
            BucketQuotaManager quotaManager) {
        this.bucketName = bucketName;
        this.quotaRoundNumber = quotaRoundNumber;
        this.quotaLimitConfig = quotaLimitConfig;
        this.quotaLimiter = quotaLimiter;
        this.quotaManager = quotaManager;
        this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
        logger.info("AsyncWriteTablePolicy init: bucketName={},quotaRoundNumber={}", bucketName,
                quotaRoundNumber);
    }

    @Override
    public void acquireQuota(long acquireObjects, long acquireSize, long maxObjects, long maxSize,
            long createTime) throws ScmServerException {
        quotaLock.lock();
        try {
            long usedObjects = quotaUsedInfo.getObjects() + quotaDeltaInfo.getObjects();
            long usedSize = quotaUsedInfo.getSize() + quotaDeltaInfo.getSize();
            QuotaHelper.checkQuota(maxObjects, maxSize, acquireObjects, acquireSize, usedObjects,
                    usedSize, bucketName);

            quotaDeltaInfo.addObjects(acquireObjects);
            quotaDeltaInfo.addSize(acquireSize);
            notifyFlushIfNeeded();
        }
        finally {
            quotaLock.unlock();
        }

    }

    @Override
    public void releaseQuota(long objects, long size, long createTime) throws ScmServerException {
        quotaLock.lock();
        try {
            quotaDeltaInfo.addObjects(-objects);
            quotaDeltaInfo.addSize(-size);
            notifyFlushIfNeeded();
        }
        finally {
            quotaLock.unlock();
        }
    }

    private void notifyFlushIfNeeded() {
        if (Math.abs(quotaDeltaInfo.getObjects()) >= quotaLimitConfig.getAsyncStrategy()
                .getMaxCacheObjects()
                || Math.abs(quotaDeltaInfo.getSize()) >= quotaLimitConfig.getAsyncStrategy()
                        .getMaxCacheSizeBytes()) {
            logger.debug("notify flush, bucketName={}, quotaRoundNumber={}, quotaInfo={}",
                    bucketName, quotaRoundNumber, quotaDeltaInfo);
            isNeedFlush = true;
            quotaManager.notifyFlush();
        }
    }

    @Override
    public void flush(boolean force) throws QuotaLimiterIncorrectException {
        if (!force && !isNeedFlush) {
            long now = System.currentTimeMillis();
            if (now - lastFlushTime < quotaLimitConfig.getAsyncStrategy().getFlushInterval()) {
                return;
            }
        }

        QuotaWrapper needFlushQuota = null;
        quotaLock.lock();
        try {
            if (quotaDeltaInfo.getObjects() == 0 && quotaDeltaInfo.getSize() == 0) {
                return;
            }
            needFlushQuota = quotaDeltaInfo;
            quotaDeltaInfo = new QuotaWrapper();
            // 先把待写表的额度提前累加到已用额度信息中，再写表，如果写表失败，则回滚
            quotaUsedInfo.addSize(needFlushQuota.getSize());
            quotaUsedInfo.addObjects(needFlushQuota.getObjects());
        }
        finally {
            quotaLock.unlock();
        }

        try {
            QuotaWrapper newQuotaInfo = quotaManager.addUsedInfoToQuotaTable(bucketName,
                    quotaRoundNumber, needFlushQuota.getObjects(), needFlushQuota.getSize());
            if (newQuotaInfo == null) {
                throw new QuotaLimiterIncorrectException(quotaLimiter);
            }
            setUsedQuota(newQuotaInfo.getObjects(), newQuotaInfo.getSize());
            lastFlushTime = System.currentTimeMillis();
            if (isNeedFlush) {
                isNeedFlush = false;
            }
        }
        catch (Exception e) {
            quotaLock.lock();
            try {
                quotaDeltaInfo.addObjects(needFlushQuota.getObjects());
                quotaDeltaInfo.addSize(needFlushQuota.getSize());
                quotaUsedInfo.addObjects(-needFlushQuota.getObjects());
                quotaUsedInfo.addSize(-needFlushQuota.getSize());
            }
            finally {
                quotaLock.unlock();
            }

            if (e instanceof QuotaLimiterIncorrectException) {
                throw (QuotaLimiterIncorrectException) e;
            }
            else {
                logger.warn(
                        "failed flush used quota, bucketName: {}, quotaRoundNumber: {}, quotaInfo: {}",
                        bucketName, quotaRoundNumber, quotaDeltaInfo, e);
            }
        }
    }

    @Override
    public String getName() {
        return QuotaLimitConfig.POLICY_ASYNC;
    }

    @Override
    public long getUsedObjectsCache() {
        return quotaDeltaInfo.getObjects();
    }

    @Override
    public long getUsedSizeCache() {
        return quotaDeltaInfo.getSize();
    }

    @Override
    public QuotaWrapper getQuotaUsedInfo() {
        return this.quotaUsedInfo;
    }

    @Override
    public void setUsedQuota(long usedObjects, long usedSize) {
        quotaLock.lock();
        try {
            this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
        }
        finally {
            quotaLock.unlock();
        }
    }
}
