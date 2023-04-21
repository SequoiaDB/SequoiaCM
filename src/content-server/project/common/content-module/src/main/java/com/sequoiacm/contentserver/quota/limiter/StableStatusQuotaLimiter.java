package com.sequoiacm.contentserver.quota.limiter;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.quota.ScmRefreshScopeRefreshedListener;
import com.sequoiacm.contentserver.quota.QuotaCacheAutoFlushable;
import com.sequoiacm.contentserver.quota.QuotaInfo;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaWrapper;
import com.sequoiacm.contentserver.quota.msg.BeginSyncMsg;
import com.sequoiacm.contentserver.quota.msg.CancelSyncMsg;
import com.sequoiacm.contentserver.quota.msg.DisableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.EnableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.FinishSyncMsg;
import com.sequoiacm.contentserver.quota.msg.QuotaMsg;
import com.sequoiacm.contentserver.quota.msg.QuotaSyncMsg;
import com.sequoiacm.contentserver.quota.msg.SyncExpiredMsg;
import com.sequoiacm.contentserver.quota.msg.SyncFailedMsg;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaQuotaAccessor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
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

    private int syncRoundNumber;

    private int quotaRoundNumber;

    private BucketQuotaManager quotaManager;

    private WaterLevelStrategy currentWaterLevelStrategy;

    private volatile QuotaWrapper quotaUsedInfo = new QuotaWrapper();

    public StableStatusQuotaLimiter(String bucketName, BucketQuotaManager quotaManager,
            int syncRoundNumber, long usedObjects, long usedSize, int quotaRoundNumber)
            throws ScmServerException {
        init(bucketName, quotaManager, syncRoundNumber, usedObjects, usedSize, quotaRoundNumber);
    }

    public StableStatusQuotaLimiter(QuotaMsg quotaMsg, BucketQuotaManager quotaManager,
            QuotaWrapper usedInfo, int quotaRoundNumber) throws ScmServerException {
        boolean msgProcessed = false;
        if (quotaMsg instanceof EnableQuotaMsg) {
            EnableQuotaMsg enableQuotaMsg = (EnableQuotaMsg) quotaMsg;
            quotaRoundNumber = enableQuotaMsg.getQuotaRoundNumber();
            msgProcessed = true;
        }

        int syncRoundNumber = -1;
        if (quotaMsg instanceof FinishSyncMsg || quotaMsg instanceof CancelSyncMsg
                || quotaMsg instanceof SyncFailedMsg || quotaMsg instanceof SyncExpiredMsg) {
            QuotaSyncMsg syncMsg = (QuotaSyncMsg) quotaMsg;
            syncRoundNumber = syncMsg.getSyncRoundNumber();
            msgProcessed = true;
        }

        if (!msgProcessed) {
            throw new ScmSystemException("unsupported quota msg: " + quotaMsg);
        }
        init(quotaMsg.getName(), quotaManager, syncRoundNumber, usedInfo.getObjects(),
                usedInfo.getSize(), quotaRoundNumber);

    }

    private void init(String bucketName, BucketQuotaManager quotaManager, int syncRoundNumber,
            long usedObjects, long usedSize, int quotaRoundNumber) throws ScmServerException {
        this.bucketName = bucketName;
        this.quotaManager = quotaManager;
        this.syncRoundNumber = syncRoundNumber;
        this.quotaLimitConfig = quotaManager.getQuotaLimitConfig();
        this.quotaUsedInfo = new QuotaWrapper(usedObjects, usedSize);
        this.quotaRoundNumber = quotaRoundNumber;
        initWaterLevel();
    }

    @Override
    public void onRefreshScopeRefreshed() throws ScmServerException {
        currentWaterLevelStrategy.refreshWriteTablePolicy();
    }

    private void initWaterLevel() throws ScmServerException {
        WaterLevel waterLevel = determineWaterLevel();
        this.currentWaterLevelStrategy = createQuotaStrategy(waterLevel);
    }

    private WaterLevelStrategy createQuotaStrategy(WaterLevel waterLevel)
            throws ScmServerException {
        if (waterLevel == WaterLevel.LOW_WATER) {
            return new LowWaterLevelStrategy();
        }
        else if (waterLevel == WaterLevel.HIGH_WATER) {
            return new HighWaterLevelStrategy();
        }
        else {
            throw new IllegalArgumentException("unknown water level: " + waterLevel);
        }
    }

    @Override
    public QuotaInfo acquireQuota(long num, long size, long createTime) throws ScmServerException {
        Lock readLock = waterLevelChangeLock.readLock();
        boolean isUnLock = false;
        try {
            readLock.lock();
            WaterLevel newWaterLevel = currentWaterLevelStrategy.acquireQuota(num, size,
                    createTime);
            if (newWaterLevel != currentWaterLevelStrategy.getWaterLevel()) {
                readLock.unlock();
                isUnLock = true;
                switchWaterLevel(newWaterLevel);
            }
            return new QuotaInfo(bucketName, num, size, createTime);
        }
        finally {
            if (!isUnLock) {
                readLock.unlock();
            }
        }
    }

    @Override
    public void releaseQuota(long num, long size, long createTime) throws ScmServerException {
        Lock readLock = waterLevelChangeLock.readLock();
        boolean isUnLock = false;
        try {
            readLock.lock();
            WaterLevel newWaterLevel = currentWaterLevelStrategy.releaseQuota(num, size,
                    createTime);
            if (newWaterLevel != currentWaterLevelStrategy.getWaterLevel()) {
                readLock.unlock();
                isUnLock = true;
                switchWaterLevel(newWaterLevel);
            }
        }
        finally {
            if (!isUnLock) {
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
            currentWaterLevelStrategy = createQuotaStrategy(newWaterLevel);
        }
        finally {
            writeLock.unlock();
        }

    }

    @Override
    public LimiterType handleMsg(QuotaMsg quotaMsg) {
        logger.info("handle msg:{}", quotaMsg);
        if (quotaMsg instanceof QuotaSyncMsg) {
            QuotaSyncMsg syncMsg = (QuotaSyncMsg) quotaMsg;
            if (syncMsg.getSyncRoundNumber() < syncRoundNumber) {
                logger.warn("bucket {} sync round number {} is less than current round number {}",
                        bucketName, syncMsg.getSyncRoundNumber(), syncRoundNumber);
                return LimiterType.NONE;
            }
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
    public boolean setUsedQuota(long usedObjects, long usedSize, int quotaRoundNumber) {
        if (this.quotaRoundNumber != quotaRoundNumber) {
            logger.warn("bucket {} quota round number {} is not equal to current quota round number {}",
                    bucketName, quotaRoundNumber, this.quotaRoundNumber);
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
    public void destroySilence() {
        try {
            flushCache(true);
        }
        catch (Exception e) {
            logger.warn("failed to flush cache:bucket={},syncRoundNumber={}", bucketName,
                    syncRoundNumber, e);
        }
    }

    @Override
    public BSONObject getInfo() {
        BSONObject info = new BasicBSONObject();
        info.put("limiter", StableStatusQuotaLimiter.class.getSimpleName());
        info.put("bucketName", bucketName);
        info.put("syncRoundNumber", syncRoundNumber);
        info.put("waterLevel", currentWaterLevelStrategy.getWaterLevel());
        info.put("writeTablePolicy", currentWaterLevelStrategy.getWriteTablePolicy().getName());
        info.put("quotaUsedInfo", quotaUsedInfo.toBSONObject());
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
    public void flushCache(boolean force) {
        currentWaterLevelStrategy.flush(force);
    }

    private enum WaterLevel {
        LOW_WATER,
        HIGH_WATER
    }

    private interface WaterLevelStrategy {

        WaterLevel acquireQuota(long num, long size, long createTime) throws ScmServerException;

        WaterLevel releaseQuota(long num, long size, long createTime) throws ScmServerException;

        void flush(boolean force);

        WaterLevel getWaterLevel();

        WriteTablePolicy getWriteTablePolicy();

        void refreshWriteTablePolicy() throws ScmServerException;
    }

    private class LowWaterLevelStrategy implements WaterLevelStrategy {

        private WriteTablePolicy writeTablePolicy;

        public LowWaterLevelStrategy() throws ScmServerException {
            logger.info("init LowWaterLevelStrategy: bucket={},syncRoundNumber={}", bucketName,
                    syncRoundNumber);
            this.writeTablePolicy = createWriteTablePolicy();
        }

        private WriteTablePolicy createWriteTablePolicy() throws ScmServerException {
            if (quotaLimitConfig.getLowWater().getPolicy().equals(QuotaLimitConfig.POLICY_SYNC)) {
                return new SyncWriteTablePolicy();
            }
            else if (quotaLimitConfig.getLowWater().getPolicy()
                    .equals(QuotaLimitConfig.POLICY_ASYNC)) {
                return new AsyncWriteTablePolicy();
            }
            else {
                throw new IllegalArgumentException(
                        "unknown policy: " + quotaLimitConfig.getLowWater().getPolicy());
            }
        }

        @Override
        public WaterLevel acquireQuota(long num, long size, long createTime)
                throws ScmServerException {
            writeTablePolicy.acquireQuota(num, size, createTime);
            return determineWaterLevel(writeTablePolicy.getUsedObjectsCache(),
                    writeTablePolicy.getUsedSizeCache());
        }

        @Override
        public WaterLevel releaseQuota(long num, long size, long createTime)
                throws ScmServerException {
            writeTablePolicy.releaseQuota(num, size, createTime);
            return determineWaterLevel(writeTablePolicy.getUsedObjectsCache(),
                    writeTablePolicy.getUsedSizeCache());
        }

        @Override
        public void flush(boolean force) {
            writeTablePolicy.flush(force);
        }

        @Override
        public WaterLevel getWaterLevel() {
            return WaterLevel.LOW_WATER;
        }

        @Override
        public WriteTablePolicy getWriteTablePolicy() {
            return writeTablePolicy;
        }

        @Override
        public void refreshWriteTablePolicy() throws ScmServerException {
            if (!writeTablePolicy.getName().equals(quotaLimitConfig.getLowWater().getPolicy())) {
                WriteTablePolicy old = this.writeTablePolicy;
                this.writeTablePolicy = createWriteTablePolicy();
                old.flush(true);
            }
        }

    }

    private class HighWaterLevelStrategy implements WaterLevelStrategy {

        private WriteTablePolicy writeTablePolicy;

        private HighWaterLevelStrategy() throws ScmServerException {
            logger.info("init HighWaterLevelStrategy: bucket={},syncRoundNumber={}", bucketName,
                    syncRoundNumber);
            writeTablePolicy = createWriteTablePolicy();
        }

        private WriteTablePolicy createWriteTablePolicy() throws ScmServerException {
            if (quotaLimitConfig.getHighWater().getPolicy().equals(QuotaLimitConfig.POLICY_SYNC)) {
                return new SyncWriteTablePolicy();
            }
            else if (quotaLimitConfig.getHighWater().getPolicy()
                    .equals(QuotaLimitConfig.POLICY_ASYNC)) {
                return new AsyncWriteTablePolicy();
            }
            else {
                throw new IllegalArgumentException(
                        "unknown policy: " + quotaLimitConfig.getHighWater().getPolicy());
            }
        }

        @Override
        public WaterLevel acquireQuota(long num, long size, long createTime)
                throws ScmServerException {
            writeTablePolicy.acquireQuota(num, size, createTime);
            return determineWaterLevel(writeTablePolicy.getUsedObjectsCache(),
                    writeTablePolicy.getUsedSizeCache());
        }

        @Override
        public WaterLevel releaseQuota(long num, long size, long createTime)
                throws ScmServerException {
            writeTablePolicy.releaseQuota(num, size, createTime);
            return determineWaterLevel(writeTablePolicy.getUsedObjectsCache(),
                    writeTablePolicy.getUsedSizeCache());
        }

        @Override
        public void flush(boolean force) {
            writeTablePolicy.flush(force);
        }

        @Override
        public WaterLevel getWaterLevel() {
            return WaterLevel.HIGH_WATER;
        }

        @Override
        public WriteTablePolicy getWriteTablePolicy() {
            return writeTablePolicy;
        }

        @Override
        public void refreshWriteTablePolicy() throws ScmServerException {
            if (!writeTablePolicy.getName().equals(quotaLimitConfig.getHighWater().getPolicy())) {
                WriteTablePolicy old = this.writeTablePolicy;
                this.writeTablePolicy = createWriteTablePolicy();
                old.flush(true);
            }
        }
    }

    private interface WriteTablePolicy {

        void acquireQuota(long num, long size, long createTime) throws ScmServerException;

        void releaseQuota(long num, long size, long createTime) throws ScmServerException;

        void flush(boolean force);

        String getName();

        long getUsedObjectsCache();

        long getUsedSizeCache();
    }

    private class SyncWriteTablePolicy implements WriteTablePolicy {
        private MetaQuotaAccessor accessor;
        private ScmLockManager lockManager;

        public SyncWriteTablePolicy() throws ScmServerException {
            this.accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getQuotaAccessor();
            this.lockManager = ScmLockManager.getInstance();
            logger.info("SyncWriteTablePolicy init, bucketName: {}, syncRoundNumber: {}", bucketName,
                    syncRoundNumber);
        }

        @Override
        public void acquireQuota(long acquireObjects, long acquireSize, long createTime)
                throws ScmServerException {
            ScmLockPath lockPath = ScmLockPathFactory
                    .createQuotaUsedLockPath(BucketQuotaManager.QUOTA_TYPE, bucketName);
            ScmLock lock = null;
            try {
                lock = lockManager.acquiresLock(lockPath);
                BSONObject record = accessor.getQuotaInfo(BucketQuotaManager.QUOTA_TYPE,
                        bucketName);
                if (record == null) {
                    logger.warn("quota record not found, bucketName: {}", bucketName);
                    return;
                }

                long usedObjects = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_OBJECTS)
                        .longValue();
                long usedSize = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_SIZE)
                        .longValue();

                checkQuota(acquireObjects, acquireSize, usedObjects, usedSize);
                BSONObject updator = buildUpdator(usedObjects + acquireObjects,
                        usedSize + acquireSize);
                accessor.updateQuotaInfo(BucketQuotaManager.QUOTA_TYPE, bucketName,
                        quotaRoundNumber, updator);
            }
            catch (ScmServerException e) {
                throw e;
            }
            catch (Exception e) {
                throw new ScmSystemException("failed to acquire quota:bucketName=" + bucketName
                        + ", acquireObjects=" + acquireObjects + ", acquireSize=" + acquireSize, e);
            }
            finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

        private BSONObject buildUpdator(long usedObjects, long usedSize) {
            BSONObject updator = new BasicBSONObject();
            updator.put(FieldName.Quota.USED_OBJECTS, Math.max(usedObjects, 0L));
            updator.put(FieldName.Quota.USED_SIZE, Math.max(usedSize, 0L));
            return updator;
        }

        @Override
        public void releaseQuota(long objects, long size, long createTime)
                throws ScmServerException {
            ScmLockPath lockPath = ScmLockPathFactory
                    .createQuotaUsedLockPath(BucketQuotaManager.QUOTA_TYPE, bucketName);
            ScmLock lock = null;
            try {
                lock = lockManager.acquiresLock(lockPath);
                BSONObject record = accessor.getQuotaInfo(BucketQuotaManager.QUOTA_TYPE,
                        bucketName);
                if (record == null) {
                    logger.warn("quota record not found, bucketName: {}, syncRoundNumber: {}",
                            bucketName, syncRoundNumber);
                    return;
                }
                long usedObjects = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_OBJECTS)
                        .longValue();
                long usedSize = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_SIZE)
                        .longValue();
                BSONObject updator = buildUpdator(usedObjects - objects, usedSize - size);
                accessor.updateQuotaInfo(BucketQuotaManager.QUOTA_TYPE, bucketName,
                        quotaRoundNumber, updator);
            }
            catch (ScmServerException e) {
                throw e;
            }
            catch (Exception e) {
                throw new ScmSystemException("failed to release quota:bucketName=" + bucketName
                        + ", objects=" + objects + ", size=" + size, e);
            }
            finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

        @Override
        public void flush(boolean force) {
            // nothing to do
        }

        @Override
        public String getName() {
            return QuotaLimitConfig.POLICY_SYNC;
        }

        @Override
        public long getUsedObjectsCache() {
            return 0;
        }

        @Override
        public long getUsedSizeCache() {
            return 0;
        }

    }

    private class AsyncWriteTablePolicy implements WriteTablePolicy {

        public AsyncWriteTablePolicy() {
            logger.info("AsyncWriteTablePolicy init: bucketName={},syncRoundNumber={}", bucketName,
                    syncRoundNumber);
        }

        private volatile QuotaWrapper quotaCache = new QuotaWrapper();

        private long lastFlushTime = System.currentTimeMillis();
        private boolean isNeedFlush = false;
        private final Lock quotaLock = new ReentrantLock();

        @Override
        public void acquireQuota(long acquireObjects, long acquireSize, long createTime)
                throws ScmServerException {
            try {
                quotaLock.lock();
                long usedObjects = quotaUsedInfo.getObjects() + quotaCache.getObjects();
                long usedSize = quotaUsedInfo.getSize() + quotaCache.getSize();
                checkQuota(acquireObjects, acquireSize, usedObjects, usedSize);
                quotaCache.addObjects(acquireObjects);
                quotaCache.addSize(acquireSize);
                notifyFlushIfNeeded();
            }
            finally {
                quotaLock.unlock();
            }

        }

        @Override
        public void releaseQuota(long objects, long size, long createTime)
                throws ScmServerException {
            try {
                quotaLock.lock();
                quotaCache.addObjects(-objects);
                quotaCache.addSize(-size);
                notifyFlushIfNeeded();
            }
            finally {
                quotaLock.unlock();
            }
        }

        private void notifyFlushIfNeeded() {
            if (Math.abs(quotaCache.getObjects()) >= quotaLimitConfig.getAsyncStrategy()
                    .getMaxCacheObjects()
                    || Math.abs(quotaCache.getSize()) >= quotaLimitConfig.getAsyncStrategy()
                            .getMaxCacheSizeBytes()) {
                logger.debug("notify flush, bucketName={}, syncRoundNumber={}, quotaInfo={}",
                        bucketName, syncRoundNumber, quotaCache);
                isNeedFlush = true;
                quotaManager.notifyFlush();
            }
        }

        @Override
        public void flush(boolean force) {
            if (!force && !isNeedFlush) {
                long now = System.currentTimeMillis();
                if (now - lastFlushTime < quotaLimitConfig.getAsyncStrategy().getFlushInterval()) {
                    return;
                }
            }

            QuotaWrapper needFlushQuota = null;
            try {
                quotaLock.lock();
                if (quotaCache.getObjects() == 0 && quotaCache.getSize() == 0) {
                    return;
                }
                needFlushQuota = quotaCache;
                quotaCache = new QuotaWrapper();
                quotaUsedInfo.addSize(needFlushQuota.getSize());
                quotaUsedInfo.addObjects(needFlushQuota.getObjects());
            }
            finally {
                quotaLock.unlock();
            }

            try {
                quotaUsedInfo = quotaManager.addUsedInfoToQuotaTable(bucketName, quotaRoundNumber,
                        needFlushQuota.getObjects(), needFlushQuota.getSize());
                lastFlushTime = System.currentTimeMillis();
                if (isNeedFlush) {
                    isNeedFlush = false;
                }
            }
            catch (Exception e) {
                try {
                    quotaLock.lock();
                    quotaCache.addObjects(needFlushQuota.getObjects());
                    quotaCache.addSize(needFlushQuota.getSize());
                    quotaUsedInfo.addObjects(-needFlushQuota.getObjects());
                    quotaUsedInfo.addSize(-needFlushQuota.getSize());
                }
                finally {
                    quotaLock.unlock();
                }
                logger.warn(
                        "failed flush used quota, bucketName: {}, quotaRoundNumber: {}, quotaInfo: {}",
                        bucketName, quotaRoundNumber, quotaCache, e);
            }
        }

        @Override
        public String getName() {
            return QuotaLimitConfig.POLICY_ASYNC;
        }

        @Override
        public long getUsedObjectsCache() {
            return quotaCache.getObjects();
        }

        @Override
        public long getUsedSizeCache() {
            return quotaCache.getSize();
        }
    }

    private WaterLevel determineWaterLevel(long cachedObjects, long cacheSizes)
            throws ScmServerException {
        long maxObjects = quotaManager.getBucketMaxObjects(bucketName);
        long maxSize = quotaManager.getBucketMaxSize(bucketName);
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

    private WaterLevel determineWaterLevel() throws ScmServerException {
        return determineWaterLevel(0, 0);
    }

    private void checkQuota(long acquireObjects, long acquireSize, long usedObjects, long usedSize)
            throws ScmServerException {
        long maxObjects = quotaManager.getBucketMaxObjects(bucketName);
        if (maxObjects >= 0) { // 小于0表示不限制
            long remainObjects = maxObjects - usedObjects;
            if (acquireObjects > remainObjects) {
                logger.warn(
                        "bucket object count exceeded,bucketName={},acquireObjects={},remainObjects={}",
                        bucketName, acquireObjects, remainObjects);
                throw new ScmServerException(ScmError.BUCKET_QUOTA_EXCEEDED,
                        "bucket quota exceeded: bucketName=" + bucketName + ", maxObjects="
                                + quotaManager.getBucketMaxObjects(bucketName));
            }
        }
        long maxSize = quotaManager.getBucketMaxSize(bucketName);
        if (maxSize >= 0) { // 小于0表示不限制
            long remainSize = maxSize - usedSize;
            if (acquireSize > remainSize) {
                logger.warn(
                        "bucket object size exceeded, bucketName={},acquireSize={},remainSize={}",
                        bucketName, acquireSize, remainSize);
                throw new ScmServerException(ScmError.BUCKET_QUOTA_EXCEEDED,
                        "bucket quota exceeded: bucketName: " + bucketName + ", maxSize: "
                                + ScmQuotaUtils.formatSize(maxSize));
            }
        }
    }
}
