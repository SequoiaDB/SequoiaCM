package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import com.sequoiacm.contentserver.config.QuotaLimitConfig;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.contentserver.service.QuotaStatisticsService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaQuotaAccessor;
import com.sequoiacm.metasource.MetaQuotaSyncAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class QuotaStatisticsServiceImpl implements QuotaStatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(QuotaStatisticsServiceImpl.class);
    @Autowired
    private IScmBucketService bucketService;

    private ThreadPoolExecutor statisticsThreadPool;

    private QuotaLimitConfig quotaLimitConfig;

    public QuotaStatisticsServiceImpl(QuotaLimitConfig quotaLimitConfig) {
        this.quotaLimitConfig = quotaLimitConfig;
        statisticsThreadPool = new ThreadPoolExecutor(1, quotaLimitConfig.getMaxStatisticsThreads(),
                60L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @EventListener
    public void handleRefreshScopeRefreshedEvent(RefreshScopeRefreshedEvent event) {
        if (quotaLimitConfig.getMaxStatisticsThreads() != statisticsThreadPool
                .getMaximumPoolSize()) {
            logger.info("quota statistics thread pool size changed from {} to {}",
                    statisticsThreadPool.getMaximumPoolSize(),
                    quotaLimitConfig.getMaxStatisticsThreads());
            statisticsThreadPool.setMaximumPoolSize(quotaLimitConfig.getMaxStatisticsThreads());
        }
    }

    @PreDestroy
    public void destroy() {
        if (statisticsThreadPool != null) {
            statisticsThreadPool.shutdown();
        }
    }

    @Override
    public void doStatistics(String type, String name, int syncRoundNumber)
            throws ScmServerException {
        ScmBucket bucket = bucketService.getBucket(name);
        ScmWorkspaceInfo workspaceInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(bucket.getWorkspace());
        try {
            MetaQuotaSyncAccessor accessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().getQuotaSyncAccessor();
            BSONObject quotaSyncInfo = accessor.getQuotaSyncInfo(type, name, syncRoundNumber);
            if (quotaSyncInfo == null) {
                throw new ScmSystemException("quota sync not exist:type=" + type + ",name=" + name
                        + ",syncRoundNumber=" + syncRoundNumber);
            }
            int quotaRoundNumber = BsonUtils
                    .getNumberChecked(quotaSyncInfo, FieldName.QuotaSync.QUOTA_ROUND_NUMBER)
                    .intValue();
            long maxCount = bucketService.getAllBucketFileCount(workspaceInfo, bucket, null, true);
            if (maxCount <= 0) {
                updateProgress(type, name, syncRoundNumber, quotaRoundNumber, 0, 0, 0, 0, true);
            }
            else {
                long agreementTime = BsonUtils
                        .getNumberChecked(quotaSyncInfo, FieldName.QuotaSync.AGREEMENT_TIME)
                        .longValue();
                statisticsThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        asyncStatistics(type, name, syncRoundNumber, quotaRoundNumber, maxCount,
                                bucket, workspaceInfo, agreementTime);
                    }
                });
            }
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to statistics:type=" + type + ",name=" + name
                    + ",syncRoundNumber+" + syncRoundNumber, e);
        }

    }

    private void asyncStatistics(String type, String name, int syncRoundNumber,
            int quotaRoundNumber, long maxCount, ScmBucket bucket, ScmWorkspaceInfo workspaceInfo,
            long agreementTime) {
        MetaCursor metaCursor = null;
        try {
            boolean isInterrupt = false;
            BSONObject selector = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_SIZE, null);
            BSONObject condition = new BasicBSONObject();
            // 获取当前秒的最大时间戳
            long time = (agreementTime / 1000) * 1000 + 999;
            condition.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME,
                    new BasicBSONObject("$lte", time));
            metaCursor = bucketService.queryAllBucketFile(workspaceInfo, bucket, condition,
                    selector, null, false);
            int processCount = 0;
            long processSize = 0;
            int flushCount = 0;
            long lastFlushTime = System.currentTimeMillis();
            long startTime = System.currentTimeMillis();
            while (metaCursor.hasNext()) {
                BSONObject bsonObject = metaCursor.getNext();
                long fileSize = BsonUtils.getLongChecked(bsonObject,
                        FieldName.FIELD_CLFILE_FILE_SIZE);
                processCount++;
                processSize += fileSize;
                if (shouldFlush(flushCount, lastFlushTime)) {
                    long currentTime = System.currentTimeMillis();
                    long processTime = currentTime - startTime;
                    long speed = processCount / (processTime > 0 ? processTime : 1);
                    long estimateTime = (maxCount - processCount) / speed;
                    boolean updated = updateProgress(type, name, syncRoundNumber, quotaRoundNumber,
                            processCount, processSize, (currentTime - startTime), estimateTime,
                            false);
                    lastFlushTime = System.currentTimeMillis();
                    flushCount++;
                    if (!updated) {
                        isInterrupt = true;
                        break;
                    }
                }
            }
            if (!isInterrupt) {
                long currentTime = System.currentTimeMillis();
                updateProgress(type, name, syncRoundNumber, quotaRoundNumber, processCount,
                        processSize, (currentTime - startTime), 0, true);
            }
        }
        catch (Exception e) {
            logger.error("failed to statistics:type=" + type + ",name=" + name + ",syncRoundNumber+"
                    + syncRoundNumber, e);
            recordErrorSilence(type, name, syncRoundNumber, e);
        }
        finally {
            if (metaCursor != null) {
                metaCursor.close();
            }
        }
    }

    private boolean shouldFlush(int flushCount, long lastFlushTime) {
        // 前三次每 1s 刷新一次，后面每 10 s 刷新一次
        if (flushCount < 3) {
            return System.currentTimeMillis() - lastFlushTime > 1000;
        }
        else {
            return System.currentTimeMillis() - lastFlushTime > 1000 * 10;
        }
    }

    private boolean updateProgress(String type, String name, int syncRoundNumber,
            int quotaRoundNumber, long processCount, long processSize, long processTime,
            long estimateTime, boolean isCompleted)
            throws ScmServerException, ScmMetasourceException {

        MetaQuotaSyncAccessor accessor = ScmContentModule.getInstance().getMetaService()
                .getMetaSource().getQuotaSyncAccessor();
        BSONObject quotaSync = accessor.getQuotaSyncInfo(type, name, syncRoundNumber);
        if (quotaSync == null) {
            logger.warn("quota sync info not exist:type=" + type + ",name=" + name
                    + ",syncRoundNumber=" + syncRoundNumber);
            return false;
        }
        ScmQuotaSyncStatus syncStatus = ScmQuotaSyncStatus
                .getByName(BsonUtils.getStringChecked(quotaSync, FieldName.QuotaSync.STATUS));
        if (syncStatus != ScmQuotaSyncStatus.SYNCING) {
            logger.warn("quota sync status is not syncing:type=" + type + ",name=" + name
                    + ",syncRoundNumber=" + syncRoundNumber + ",syncStatus=" + syncStatus);
            return false;
        }
        BSONObject updator = new BasicBSONObject();
        updator.put(FieldName.QuotaSync.STATISTICS_OBJECTS, processCount);
        updator.put(FieldName.QuotaSync.STATISTICS_SIZE, processSize);
        updator.put(FieldName.QuotaSync.STATISTICS_DETAIL + "."
                + FieldName.QuotaStatisticsProgress.PROCESS_TIME, processTime);
        updator.put(FieldName.QuotaSync.STATISTICS_DETAIL + "."
                + FieldName.QuotaStatisticsProgress.ESTIMATED_TIME, estimateTime);
        if (isCompleted) {
            updator.put(
                    FieldName.QuotaSync.STATISTICS_DETAIL + "."
                            + FieldName.QuotaStatisticsProgress.STATUS,
                    CommonDefine.QuotaStatisticsStatus.FINISH);
        }
        accessor.updateQuotaSyncInfo(type, name, syncRoundNumber, updator);
        if (isCompleted) {
            updateUsedQuota(type, name, processCount, processSize, quotaRoundNumber);
        }
        return true;

    }

    private static void updateUsedQuota(String type, String name, long processCount,
            long processSize, int quotaRoundNumber)
            throws ScmServerException, ScmMetasourceException {
        ScmLock lock = null;
        try {
            MetaQuotaAccessor quotaAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().getQuotaAccessor();
            ScmLockManager lockManager = ScmLockManager.getInstance();
            ScmLockPath lockPath = ScmLockPathFactory.createQuotaUsedLockPath(type, name);
            lock = lockManager.acquiresLock(lockPath);
            BSONObject quota = quotaAccessor.getQuotaInfo(type, name);
            if (quota == null) {
                logger.warn("quota not exist, ignore update:type=" + type + ",name=" + name);
                return;
            }
            BSONObject quotaUpdator = new BasicBSONObject();
            quotaUpdator.put(FieldName.Quota.USED_OBJECTS, processCount);
            quotaUpdator.put(FieldName.Quota.USED_SIZE, processSize);
            quotaAccessor.updateQuotaInfo(type, name, quotaRoundNumber, quotaUpdator);
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private void recordErrorSilence(String type, String name, int syncRoundNumber, Exception e) {
        try {
            MetaQuotaSyncAccessor accessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().getQuotaSyncAccessor();
            BSONObject quotaSyncInfo = accessor.getQuotaSyncInfo(type, name, syncRoundNumber);
            if (quotaSyncInfo == null) {
                logger.warn("quota sync info not exist,ignore record error:type=" + type + ",name="
                        + name + ",syncRoundNumber=" + syncRoundNumber);
                return;
            }
            ScmQuotaSyncStatus syncStatus = ScmQuotaSyncStatus.getByName(
                    BsonUtils.getStringChecked(quotaSyncInfo, FieldName.QuotaSync.STATUS));
            if (syncStatus != ScmQuotaSyncStatus.SYNCING) {
                logger.warn("quota sync status is not syncing,ignore record error:type=" + type
                        + ",name=" + name + ",syncRoundNumber=" + syncRoundNumber + ",syncStatus="
                        + syncStatus);
                return;
            }
            BSONObject updator = new BasicBSONObject();
            updator.put(
                    FieldName.QuotaSync.STATISTICS_DETAIL + "."
                            + FieldName.QuotaStatisticsProgress.STATUS,
                    CommonDefine.QuotaStatisticsStatus.FAILED);
            updator.put(
                    FieldName.QuotaSync.STATISTICS_DETAIL + "."
                            + FieldName.QuotaStatisticsProgress.ERROR_MSG,
                    "statistics error:" + e.getMessage());
            accessor.updateQuotaSyncInfo(type, name, syncRoundNumber, updator);
        }
        catch (Exception ex) {
            logger.warn("failed to record error:type=" + type + ",name=" + name
                    + ",syncRoundNumber+" + syncRoundNumber, ex);
        }
    }

}
