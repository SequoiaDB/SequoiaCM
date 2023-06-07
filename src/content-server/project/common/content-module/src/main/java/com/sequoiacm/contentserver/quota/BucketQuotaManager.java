package com.sequoiacm.contentserver.quota;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.quota.limiter.LimiterType;
import com.sequoiacm.contentserver.quota.limiter.stable.StableStatusQuotaLimiter;
import com.sequoiacm.contentserver.quota.limiter.SyncingStatusQuotaLimiter;
import com.sequoiacm.contentserver.quota.limiter.UnlimitedStatusQuotaLimiter;
import com.sequoiacm.contentserver.quota.msg.DisableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.EnableQuotaMsg;
import com.sequoiacm.contentserver.quota.msg.QuotaMsg;
import com.sequoiacm.contentserver.quota.limiter.QuotaLimiter;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmHashSlotLock;
import com.sequoiacm.infrastructure.common.ScmLockWrapper;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketDeletedEvent;
import com.sequoiacm.infrastructure.config.client.core.quota.EnableQuotaSubscriber;
import com.sequoiacm.infrastructure.config.client.core.quota.QuotaChangeEvent;
import com.sequoiacm.infrastructure.config.client.core.quota.QuotaConfSubscriber;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.IndexDef;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaQuotaAccessor;
import com.sequoiacm.metasource.MetaQuotaSyncAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableQuotaSubscriber
public class BucketQuotaManager implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(BucketQuotaManager.class);

    public static final String QUOTA_TYPE = "bucket";

    private final Map<String, QuotaLimiter> bucketQuotaLimiter = new ConcurrentHashMap<>();

    // 从 bucketQuotaLimiter 获取 limiter 时，需要加读锁，变更时需要加写锁
    private ScmHashSlotLock limiterLock;

    private ScmTimer refreshSyncInfoTimer;
    private RefreshSyncInfoTask refreshSyncInfoTask;

    private FlushAndRefreshQuotaTask flushAndRefreshQuotaTask;

    // Dummy value to associate with an Object in the syncingStatusBuckets Map
    private static final Object PRESENT = new Object();
    private Map<String, Object> syncingStatusBuckets = new ConcurrentHashMap<>();

    @Autowired
    private QuotaConfSubscriber quotaConfSubscriber;

    @Autowired
    private QuotaLimitConfig quotaLimitConfig;

    @Autowired
    private BucketInfoManager bucketInfoManager;

    private MetaQuotaSyncAccessor quotaSyncAccessor;
    private MetaQuotaAccessor quotaAccessor;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        this.init();
    }

    public void init() throws Exception {
        try {
            limiterLock = new ScmHashSlotLock(2000);
            quotaSyncAccessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getQuotaSyncAccessor();
            quotaAccessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getQuotaAccessor();
            ensureTable();
            setupRefreshSyncInfoTask();
            flushAndRefreshQuotaTask = new FlushAndRefreshQuotaTask();
            flushAndRefreshQuotaTask.start();
        }
        catch (Exception e) {
            destroy();
            throw e;
        }
    }

    private void setupRefreshSyncInfoTask() {
        refreshSyncInfoTask = new RefreshSyncInfoTask(
                quotaLimitConfig.getRefreshSyncInfoInterval());
        refreshSyncInfoTimer = ScmTimerFactory.createScmTimer(1);
        refreshSyncInfoTimer.schedule(refreshSyncInfoTask,
                refreshSyncInfoTask.getRefreshSyncInfoInterval(),
                refreshSyncInfoTask.getRefreshSyncInfoInterval());
    }

    private void ensureTable() throws ScmMetasourceException {
        IndexDef indexDef = new IndexDef();
        indexDef.setUnionKeys(Arrays.asList(FieldName.Quota.TYPE, FieldName.Quota.NAME));
        indexDef.setUnique(true);
        quotaSyncAccessor.ensureTable(Collections.singletonList(indexDef));
        quotaAccessor.ensureTable(Collections.singletonList(indexDef));
    }

    @PreDestroy
    public void destroy() {
        if (refreshSyncInfoTimer != null) {
            refreshSyncInfoTimer.cancel();
        }
        if (flushAndRefreshQuotaTask != null) {
            flushAndRefreshQuotaTask.stopAndWaitExit();
        }
        for (QuotaLimiter limiter : bucketQuotaLimiter.values()) {
            limiter.destroySilence();
        }
    }

    @SlowLog(operation = "acquireQuota")
    public QuotaInfo acquireQuota(String bucketName, long objects, long size, Long createTime)
            throws ScmServerException {
        checkArg(bucketName, objects, size);
        if (createTime == null) {
            createTime = System.currentTimeMillis();
        }
        ScmLockWrapper lock = limiterLock.getReadLock(bucketName);
        try {
            lock.lock();
            QuotaLimiter quotaLimiter = getQuotaLimiter(bucketName, lock);
            return quotaLimiter.acquireQuota(objects, size, createTime);
        }
        catch (QuotaLimiterIncorrectException e) {
            lock.unlock();
            lock = null;
            removeQuotaLimiterAndDestroyInLock(bucketName, e.getCurrentQuotaLimiter());
            return acquireQuota(bucketName, objects, size, createTime);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to acquire quota:bucket=" + bucketName
                    + ",objects=" + objects + ",size=" + size, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public QuotaInfo acquireQuota(long bucketId, long size, Long createTime)
            throws ScmServerException {
        ScmBucket bucket = bucketInfoManager.getBucketById(bucketId);
        if (bucket == null) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                    "bucket not found, bucketId: " + bucketId);
        }
        return acquireQuota(bucket.getName(), 1, size, createTime);
    }

    public QuotaInfo acquireQuota(String bucketName, long size, Long createTime)
            throws ScmServerException {
        return acquireQuota(bucketName, 1, size, createTime);
    }

    public void releaseQuota(long bucketId, long size, long createTime) throws ScmServerException {
        ScmBucket bucket = bucketInfoManager.getBucketById(bucketId);
        if (bucket == null) {
            logger.warn("bucket not found, bucketId: {}", bucketId);
            return;
        }
        releaseQuota(bucket.getName(), 1, size, createTime);
    }

    @SlowLog(operation = "releaseQuota")
    public void releaseQuota(String bucketName, long objects, long size, long createTime) {
        checkArg(bucketName, objects, size);
        ScmLockWrapper lock = limiterLock.getReadLock(bucketName);
        try {
            lock.lock();
            QuotaLimiter quotaLimiter = getQuotaLimiter(bucketName, lock);
            quotaLimiter.releaseQuota(objects, size, createTime);
        }
        catch (QuotaLimiterIncorrectException e) {
            lock.unlock();
            lock = null;
            removeQuotaLimiterAndDestroyInLock(bucketName, e.getCurrentQuotaLimiter());
            releaseQuota(bucketName, objects, size, createTime);
        }
        catch (Exception e) {
            logger.warn("release quota failed: bucketName={},objects={},size={},createTime={}",
                    bucketName, objects, size, createTime, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public void releaseQuota(QuotaInfo quotaInfo) {
        if (quotaInfo == null) {
            return;
        }
        releaseQuota(quotaInfo.getBucketName(), quotaInfo.getObjects(), quotaInfo.getSize(),
                quotaInfo.getCreateTime());
    }

    public void handleMsg(QuotaMsg quotaMsg) throws ScmServerException {
        logger.info("receive msg:{}", quotaMsg);
        String bucketName = quotaMsg.getName();
        ScmLockWrapper lock = limiterLock.getWriteLock(bucketName);
        try {
            lock.lock();
            QuotaLimiter quotaLimiter = getQuotaLimiter(bucketName, lock);
            LimiterType limiterType = quotaLimiter.handleMsg(quotaMsg);
            if (limiterType != null && limiterType != quotaLimiter.getType()) {
                logger.info("quotaLimiter change:old={},new={},msg={}", quotaLimiter.getType(),
                        limiterType, quotaMsg);
                QuotaLimiter newLimiter = createQuotaLimiter(bucketName, quotaLimiter, limiterType,
                        quotaMsg);
                quotaLimiter.beforeLimiterChange(quotaMsg);
                if (newLimiter == null) {
                    removeQuotaLimiter(bucketName);
                }
                else {
                    putQuotaLimiter(bucketName, newLimiter);
                }
                quotaLimiter.afterLimiterChange(quotaMsg);
                lock.unlock();
                lock = null;
                quotaLimiter.destroySilence();
            }
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to handle msg:" + quotaMsg, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private void putQuotaLimiter(String bucketName, QuotaLimiter newLimiter) {
        QuotaLimiter oldLimiter = bucketQuotaLimiter.put(bucketName, newLimiter);
        if (newLimiter.getType() == LimiterType.SYNCING) {
            syncingStatusBuckets.put(bucketName, PRESENT);
        }
        if (oldLimiter != null && oldLimiter.getType() == LimiterType.SYNCING) {
            syncingStatusBuckets.remove(bucketName);
        }
    }

    private QuotaLimiter createQuotaLimiter(String bucketName, QuotaLimiter oldQuotaLimiter,
            LimiterType newLimiterType, QuotaMsg quotaMsg) throws ScmServerException {
        if (newLimiterType == LimiterType.STABLE) {
            return new StableStatusQuotaLimiter(quotaMsg, this, oldQuotaLimiter.getQuotaUsedInfo());
        }
        else if (newLimiterType == LimiterType.SYNCING) {
            return new SyncingStatusQuotaLimiter(quotaMsg, this,
                    oldQuotaLimiter.getQuotaUsedInfo());
        }
        else if (newLimiterType == LimiterType.UNLIMITED) {
            return new UnlimitedStatusQuotaLimiter(bucketName, this);
        }
        else if (newLimiterType == LimiterType.NONE) {
            return null;
        }
        else {
            throw new IllegalArgumentException("unknown limiterType:" + newLimiterType);
        }
    }

    public QuotaLimiter removeQuotaLimiter(String bucketName) {
        QuotaLimiter quotaLimiter = bucketQuotaLimiter.remove(bucketName);
        if (quotaLimiter != null) {
            logger.info("remove quota limiter:{}", quotaLimiter.getInfo().toString());
        }
        if (quotaLimiter != null && quotaLimiter.getType() == LimiterType.SYNCING) {
            syncingStatusBuckets.remove(bucketName);
        }
        return quotaLimiter;
    }

    public void removeQuotaLimiterAndDestroyInLock(String bucketName,
            QuotaLimiter expectQuotaLimiter) {
        QuotaLimiter removedQuotaLimiter = null;
        ScmLockWrapper writeLock = limiterLock.getWriteLock(bucketName);
        try {
            writeLock.lock();
            QuotaLimiter currentQuotaLimiter = bucketQuotaLimiter.get(bucketName);
            if (currentQuotaLimiter == expectQuotaLimiter) {
                removedQuotaLimiter = removeQuotaLimiter(bucketName);
            }
        }
        finally {
            writeLock.unlock();
        }
        if (removedQuotaLimiter != null) {
            removedQuotaLimiter.destroySilence();
        }
    }

    public BSONObject getQuotaLimiterInfo(String type, String name) throws ScmServerException {
        if (!QUOTA_TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be bucket");
        }

        ScmLockWrapper readLock = limiterLock.getReadLock(name);
        try {
            readLock.lock();
            QuotaLimiter bucketQuotaLimiter = getQuotaLimiter(name, readLock);
            return bucketQuotaLimiter.getInfo();
        }
        finally {
            readLock.unlock();
        }

    }

    private void checkArg(String bucketName, long objects, long size) {
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("bucketName is null or empty");
        }
        if (objects < 0) {
            throw new IllegalArgumentException("objects can not be less than 0");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size can not be less than 0");
        }
    }

    private QuotaLimiter getQuotaLimiter(String bucketName, ScmLockWrapper outerLock)
            throws ScmServerException {
        QuotaLimiter quotaLimiter = bucketQuotaLimiter.get(bucketName);
        if (quotaLimiter != null) {
            return quotaLimiter;
        }

        // 外层是写锁，直接创建 limiter
        if (outerLock.isWriteLock()) {
            return createQuotaLimierIfAbsent(bucketName);
        }

        // 外层是读锁，需要暂时把读锁释放，加写锁后创建 limiter
        outerLock.unlock();
        ScmLockWrapper writeLock = limiterLock.getWriteLock(bucketName);
        try {
            writeLock.lock();
            return createQuotaLimierIfAbsent(bucketName);
        }
        finally {
            writeLock.unlock();
            outerLock.lock(); // 需要把外层的锁复原
        }
    }

    private QuotaLimiter createQuotaLimierIfAbsent(String bucketName) throws ScmServerException {
        QuotaLimiter quotaLimiter;
        quotaLimiter = bucketQuotaLimiter.get(bucketName);
        if (quotaLimiter != null) {
            return quotaLimiter;
        }
        quotaLimiter = initQuotaLimiter(bucketName);
        putQuotaLimiter(bucketName, quotaLimiter);
        return quotaLimiter;
    }

    private QuotaLimiter initQuotaLimiter(String bucketName) throws ScmServerException {
        QuotaConfigDetail quotaConfigDetail = getQuotaConfigDetail(bucketName);
        if (quotaConfigDetail == null || !quotaConfigDetail.isEnable()) {
            return new UnlimitedStatusQuotaLimiter(bucketName, this);
        }

        long usedObjects = quotaConfigDetail.getUsedObjects();
        long usedSize = quotaConfigDetail.getUsedSize();
        QuotaSyncInfo quotaSyncInfo = getBucketSyncInfo(bucketName);
        if (quotaSyncInfo == null) {
            // 可能刚开启限额还未触发同步，没有统计信息
            return new StableStatusQuotaLimiter(bucketName, this, usedObjects, usedSize,
                    quotaConfigDetail.getQuotaRoundNumber());
        }
        QuotaLimiter quotaLimiter = null;
        ScmQuotaSyncStatus syncStatus = ScmQuotaSyncStatus.getByName(quotaSyncInfo.getStatus());
        if (syncStatus == ScmQuotaSyncStatus.SYNCING) {
            SyncingStatusQuotaLimiter syncingStatusQuotaLimiter = new SyncingStatusQuotaLimiter(
                    bucketName, this, quotaSyncInfo.getSyncRoundNumber(),
                    quotaSyncInfo.getExpireTime(), usedObjects, usedSize,
                    quotaSyncInfo.getQuotaRoundNumber());
            if (quotaSyncInfo.getAgreementTime() != null) {
                syncingStatusQuotaLimiter.setAgreementTime(quotaSyncInfo.getAgreementTime());
            }
            quotaLimiter = syncingStatusQuotaLimiter;
        }
        else if (syncStatus == ScmQuotaSyncStatus.FAILED
                || syncStatus == ScmQuotaSyncStatus.CANCELED
                || syncStatus == ScmQuotaSyncStatus.COMPLETED) {
            quotaLimiter = new StableStatusQuotaLimiter(bucketName, this, usedObjects, usedSize,
                    quotaConfigDetail.getQuotaRoundNumber());
        }
        else {
            throw new ScmSystemException("invalid sync status: " + syncStatus);
        }
        return quotaLimiter;
    }

    public QuotaWrapper getStatisticsQuotaInfo(String bucketName, int syncRoundNumber)
            throws ScmSystemException {
        QuotaWrapper statisticsQuotaInfo = new QuotaWrapper();
        QuotaSyncInfo bucketSyncInfo = getBucketSyncInfo(bucketName);
        if (bucketSyncInfo == null) {
            logger.warn("bucket quota sync info is null:bucketName={}", bucketName);
            return null;
        }
        if (bucketSyncInfo.getSyncRoundNumber() != syncRoundNumber) {
            logger.warn(
                    "bucket quota sync info is not match:bucketName={},syncRoundNumber={},currentRoundNumber={}",
                    bucketName, syncRoundNumber, bucketSyncInfo.getSyncRoundNumber());
            return null;
        }
        statisticsQuotaInfo.setObjects(bucketSyncInfo.getStatisticsObjects());
        statisticsQuotaInfo.setSize(bucketSyncInfo.getStatisticsSize());
        return statisticsQuotaInfo;
    }

    public Long getBucketMaxObjects(String bucketName, int quotaRoundNumber)
            throws ScmSystemException {
        try {
            QuotaConfig quota = quotaConfSubscriber.getQuota(QUOTA_TYPE, bucketName);
            if (quota == null || quota.getQuotaRoundNumber() != quotaRoundNumber) {
                return null;
            }
            return quota.getMaxObjects();
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "failed to get bucket quota config:bucketName=" + bucketName, e);
        }
    }

    public Long getBucketMaxSize(String bucketName, int quotaRoundNumber)
            throws ScmSystemException {
        try {
            QuotaConfig quota = quotaConfSubscriber.getQuota(QUOTA_TYPE, bucketName);
            if (quota == null || quota.getQuotaRoundNumber() != quotaRoundNumber) {
                return null;
            }
            return quota.getMaxSize();
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "failed to get bucket quota config:bucketName=" + bucketName, e);
        }
    }

    public QuotaConfig getQuotaConfig(String bucketName, int quotaRoundNumber)
            throws ScmSystemException {
        try {
            QuotaConfig quota = quotaConfSubscriber.getQuota(QUOTA_TYPE, bucketName);
            if (quota == null || quota.getQuotaRoundNumber() != quotaRoundNumber) {
                return null;
            }
            return quota;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "failed to get bucket quota config:bucketName=" + bucketName, e);
        }
    }

    public void notifyFlush() {
        flushAndRefreshQuotaTask.wakeup();
    }

    private Map<String, QuotaSyncInfo> loadBucketSyncingStatusInfo() throws ScmMetasourceException {
        MetaCursor cursor = null;
        Map<String, QuotaSyncInfo> bucketQuotaSyncInfo = new ConcurrentHashMap<>();
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.QuotaSync.TYPE, QUOTA_TYPE);
            matcher.put(FieldName.QuotaSync.STATUS, ScmQuotaSyncStatus.SYNCING.getName());
            cursor = quotaSyncAccessor.query(matcher, null, null);
            while (cursor.hasNext()) {
                BSONObject bson = cursor.getNext();
                QuotaSyncInfo quotaSyncInfo = new QuotaSyncInfo(bson);
                bucketQuotaSyncInfo.put(quotaSyncInfo.getName(), quotaSyncInfo);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bucketQuotaSyncInfo;
    }

    private QuotaSyncInfo getBucketSyncInfo(String bucketName) throws ScmSystemException {
        try {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.QuotaSync.TYPE, QUOTA_TYPE);
            matcher.put(FieldName.QuotaSync.NAME, bucketName);
            BSONObject bson = quotaSyncAccessor.queryOne(matcher, null, null);
            if (bson == null) {
                return null;
            }
            return new QuotaSyncInfo(bson);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "failed to get bucket quota sync info:bucketName=" + bucketName, e);
        }
    }

    public QuotaConfigDetail getQuotaConfigDetail(String bucketName) throws ScmSystemException {
        try {
            BSONObject quotaInfo = quotaAccessor.getQuotaInfo(QUOTA_TYPE, bucketName);
            if (quotaInfo == null) {
                return null;
            }
            return new QuotaConfigDetail(quotaInfo);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "failed to get bucket quota config:bucketName=" + bucketName, e);
        }
    }

    public QuotaWrapper addUsedInfoToQuotaTable(String bucketName, int quotaRoundNumber,
            long usedObjects, long usedSize) throws ScmSystemException {
        logger.debug(
                "add used info to quota table:bucketName={},quotaRoundNumber={},usedObjects={},usedSize={}",
                bucketName, quotaRoundNumber, usedObjects, usedSize);
        ScmLock lock = null;
        ScmLockPath lockPath = ScmLockPathFactory.createQuotaUsedLockPath(QUOTA_TYPE, bucketName);
        try {
            ScmLockManager lockManager = ScmLockManager.getInstance();
            lock = lockManager.acquiresLock(lockPath);

            BSONObject record = quotaAccessor.getQuotaInfo(QUOTA_TYPE, bucketName);
            if (record == null) {
                logger.warn(
                        "quota info not found, ignore to update:bucketName={},quotaRoundNumber={},usedObjects={},usedSize={}",
                        bucketName, quotaRoundNumber, usedObjects, usedSize);
                return null;
            }
            int currentQuotaRoundNumber = BsonUtils
                    .getNumberChecked(record, FieldName.Quota.QUOTA_ROUND_NUMBER).intValue();
            if (currentQuotaRoundNumber != quotaRoundNumber) {
                logger.warn(
                        "quota round number not match, ignore to update:bucketName={},quotaRoundNumber={},currentQuotaRoundNumber={},usedObjects={},usedSize={}",
                        bucketName, quotaRoundNumber, currentQuotaRoundNumber, usedObjects,
                        usedSize);
                return null;
            }

            long newSize = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_SIZE).longValue()
                    + usedSize;
            long newObjects = BsonUtils.getNumberChecked(record, FieldName.Quota.USED_OBJECTS)
                    .longValue() + usedObjects;

            if (newSize < 0 || newObjects < 0) {
                logger.info(
                        "the quota info to be updated is less than 0:bucketName={},quotaRoundNumber={},usedObjects={},usedSize={}",
                        bucketName, quotaRoundNumber, usedObjects, usedSize);
            }
            BSONObject updator = new BasicBSONObject();
            updator.put(FieldName.Quota.USED_SIZE, newSize);
            updator.put(FieldName.Quota.USED_OBJECTS, newObjects);
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.Quota.TYPE, QUOTA_TYPE);
            matcher.put(FieldName.Quota.NAME, bucketName);
            matcher.put(FieldName.Quota.QUOTA_ROUND_NUMBER, quotaRoundNumber);
            BSONObject bsonObject = quotaAccessor.queryAndUpdate(matcher,
                    new BasicBSONObject("$set", updator), null, true);
            if (bsonObject != null) {
                QuotaWrapper quotaUsedInfo = new QuotaWrapper();
                quotaUsedInfo.setObjects(BsonUtils
                        .getNumberChecked(bsonObject, FieldName.Quota.USED_OBJECTS).longValue());
                quotaUsedInfo.setSize(BsonUtils
                        .getNumberChecked(bsonObject, FieldName.Quota.USED_SIZE).longValue());
                return quotaUsedInfo;
            }
            else {
                logger.warn(
                        "quota info not found, ignore to update:bucketName={},quotaRoundNumber={},usedObjects={},usedSize={}",
                        bucketName, quotaRoundNumber, usedObjects, usedSize);
                return null;
            }
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to update quota info:bucketName=" + bucketName
                    + ",quotaRoundNumber=" + quotaRoundNumber, e);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @EventListener
    public void handleQuotaChange(QuotaChangeEvent quotaChangeEvent) throws ScmServerException {
        logger.info("handle quota change event:{}", quotaChangeEvent);
        if (quotaChangeEvent.getType().equals(QUOTA_TYPE)) {
            QuotaConfig newQuotaConfig = quotaChangeEvent.getNewQuotaConfig();
            if (newQuotaConfig == null) {
                handleMsg(new DisableQuotaMsg(QUOTA_TYPE, quotaChangeEvent.getName(),
                        Integer.MAX_VALUE));
                return;
            }
            if (newQuotaConfig.isEnable()) {
                handleMsg(new EnableQuotaMsg(QUOTA_TYPE, quotaChangeEvent.getName(),
                        newQuotaConfig.getQuotaRoundNumber()));
            }
            else {
                handleMsg(new DisableQuotaMsg(QUOTA_TYPE, quotaChangeEvent.getName(),
                        newQuotaConfig.getQuotaRoundNumber()));
            }
        }
    }

    @EventListener
    public void handleBucketDeletedEvent(BucketDeletedEvent bucketDeletedEvent)
            throws ScmServerException {
        logger.info("handle bucket deleted event:{}", bucketDeletedEvent);
        DisableQuotaMsg disableQuotaMsg = new DisableQuotaMsg(QUOTA_TYPE,
                bucketDeletedEvent.getDeletedBucketName(), Integer.MAX_VALUE);
        handleMsg(disableQuotaMsg);
    }

    @EventListener
    public void handleRefreshScopeRefreshedEvent(RefreshScopeRefreshedEvent event)
            throws ScmServerException {
        logger.info("handle RefreshScopeRefreshedEvent:{}", event);
        for (QuotaLimiter limiter : bucketQuotaLimiter.values()) {
            if (limiter instanceof ScmRefreshScopeRefreshedListener) {
                try {
                    ((ScmRefreshScopeRefreshedListener) limiter).onRefreshScopeRefreshed();
                }
                catch (QuotaLimiterIncorrectException e) {
                    removeQuotaLimiterAndDestroyInLock(e.getCurrentQuotaLimiter().getBucketName(),
                            e.getCurrentQuotaLimiter());
                }

            }
        }
        if (refreshSyncInfoTask != null && refreshSyncInfoTask
                .getRefreshSyncInfoInterval() != quotaLimitConfig.getRefreshSyncInfoInterval()) {
            logger.info("update refreshSyncInfoInterval to {}",
                    quotaLimitConfig.getRefreshSyncInfoInterval() + "ms");
            if (refreshSyncInfoTimer != null) {
                refreshSyncInfoTimer.cancel();
            }
            setupRefreshSyncInfoTask();
        }
    }

    public QuotaLimitConfig getQuotaLimitConfig() {
        return quotaLimitConfig;
    }

    public void setQuotaLimitConfig(QuotaLimitConfig quotaLimitConfig) {
        this.quotaLimitConfig = quotaLimitConfig;
    }

    private class RefreshSyncInfoTask extends ScmTimerTask {
        private long refreshSyncInfoInterval;

        public RefreshSyncInfoTask(long refreshSyncInfoInterval) {
            this.refreshSyncInfoInterval = refreshSyncInfoInterval;
        }

        public long getRefreshSyncInfoInterval() {
            return refreshSyncInfoInterval;
        }

        @Override
        public void run() {
            try {
                Map<String, QuotaSyncInfo> currentSyncingStatusInfo = loadBucketSyncingStatusInfo();
                Set<String> currentSyncingStatusBuckets = new HashSet<>(
                        syncingStatusBuckets.keySet());
                // 1. 表里面是同步状态，但是内存里面没有
                for (String bucket : currentSyncingStatusInfo.keySet()) {
                    if (!currentSyncingStatusBuckets.contains(bucket)) {
                        QuotaLimiter removedLimiter = null;
                        ScmLockWrapper writeLock = limiterLock.getWriteLock(bucket);
                        try {
                            writeLock.lock();
                            QuotaSyncInfo bucketSyncInfo = getBucketSyncInfo(bucket);
                            boolean isSyncing = bucketSyncInfo != null && bucketSyncInfo.getStatus()
                                    .equals(ScmQuotaSyncStatus.SYNCING.getName());
                            if (!syncingStatusBuckets.containsKey(bucket) && isSyncing) {
                                logger.warn(
                                        "quota sync status is not match:record status is syncing, but local is not, bucket:{},",
                                        bucket);
                                removedLimiter = removeQuotaLimiter(bucket);
                            }
                        }
                        finally {
                            writeLock.unlock();
                        }
                        if (removedLimiter != null) {
                            removedLimiter.destroySilence();
                        }
                    }
                }

                // 2. 内存里面是同步状态，但是表里面没有
                for (String bucket : currentSyncingStatusBuckets) {
                    if (!currentSyncingStatusInfo.containsKey(bucket)) {
                        QuotaLimiter removedLimiter = null;
                        ScmLockWrapper writeLock = limiterLock.getWriteLock(bucket);
                        try {
                            writeLock.lock();
                            QuotaSyncInfo bucketSyncInfo = getBucketSyncInfo(bucket);
                            boolean isSyncing = bucketSyncInfo != null && bucketSyncInfo.getStatus()
                                    .equals(ScmQuotaSyncStatus.SYNCING.getName());
                            if (syncingStatusBuckets.containsKey(bucket) && !isSyncing) {
                                logger.warn(
                                        "quota sync status is not match:local is syncing, but record is not, bucket:{},",
                                        bucket);
                                removedLimiter = removeQuotaLimiter(bucket);
                            }
                        }
                        finally {
                            writeLock.unlock();
                        }
                        if (removedLimiter != null) {
                            removedLimiter.destroySilence();
                        }
                    }
                }

            }
            catch (Exception e) {
                logger.error("failed to refresh bucket quota sync info", e);
            }

        }
    }

    private class FlushAndRefreshQuotaTask extends Thread {

        private volatile boolean isExit = false;

        @Override
        public void run() {
            while (true) {
                try {
                    if (isExit) {
                        flushQuotaSilence();
                        break;
                    }
                    flushQuotaSilence();
                    refreshQuotaSilence();
                    waitNextExecute(quotaLimitConfig.getAsyncStrategy().getFlushInterval());
                }
                catch (Throwable e) {
                    logger.error("failed to flush or refresh quota", e);
                }
            }
        }

        private void flushQuotaSilence() {
            for (QuotaLimiter limiter : bucketQuotaLimiter.values()) {
                if (limiter instanceof QuotaCacheAutoFlushable) {
                    ScmLockWrapper readLock = limiterLock.getReadLock(limiter.getBucketName());
                    try {
                        readLock.lock();
                        ((QuotaCacheAutoFlushable) limiter).flushCache(isExit);
                    }
                    catch (QuotaLimiterIncorrectException e) {
                        readLock.unlock();
                        readLock = null;
                        removeQuotaLimiterAndDestroyInLock(limiter.getBucketName(),
                                e.getCurrentQuotaLimiter());
                    }
                    catch (Exception e) {
                        logger.error("failed to flush quota cache", e);
                    }
                    finally {
                        if (readLock != null) {
                            readLock.unlock();
                        }
                    }
                }
            }
        }

        private void refreshQuotaSilence() {
            MetaCursor cursor = null;
            try {
                BSONObject matcher = new BasicBSONObject();
                matcher.put(FieldName.Quota.TYPE, QUOTA_TYPE);
                cursor = quotaAccessor.query(matcher, null, null);
                while (cursor.hasNext()) {
                    QuotaConfigDetail quotaConfigDetail = new QuotaConfigDetail(cursor.getNext());
                    ScmLockWrapper readLock = limiterLock.getReadLock(quotaConfigDetail.getName());
                    try {
                        readLock.lock();
                        QuotaLimiter quotaLimiter = bucketQuotaLimiter
                                .get(quotaConfigDetail.getName());
                        if (quotaLimiter != null) {
                            quotaLimiter.setUsedQuota(quotaConfigDetail.getUsedObjects(),
                                    quotaConfigDetail.getUsedSize(),
                                    quotaConfigDetail.getQuotaRoundNumber());
                        }
                    }
                    catch (QuotaLimiterIncorrectException e) {
                        readLock.unlock();
                        readLock = null;
                        removeQuotaLimiterAndDestroyInLock(
                                e.getCurrentQuotaLimiter().getBucketName(),
                                e.getCurrentQuotaLimiter());
                    }
                    finally {
                        if (readLock != null) {
                            readLock.unlock();
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.warn("failed to refresh quota", e);
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private synchronized void waitNextExecute(long time) {
            try {
                this.wait(time);
            }
            catch (InterruptedException e) {
                logger.error("failed to wait", e);
            }
        }

        public synchronized void wakeup() {
            this.notify();
        }

        public void stopAndWaitExit() {
            isExit = true;
            wakeup();
        }
    }
}
