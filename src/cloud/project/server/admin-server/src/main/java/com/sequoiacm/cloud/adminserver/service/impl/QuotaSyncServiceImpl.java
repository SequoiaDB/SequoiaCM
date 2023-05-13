package com.sequoiacm.cloud.adminserver.service.impl;

import com.sequoiacm.cloud.adminserver.config.QuotaSyncConfig;
import com.sequoiacm.cloud.adminserver.core.QuotaHelper;
import com.sequoiacm.cloud.adminserver.core.QuotaSyncMsgSender;
import com.sequoiacm.cloud.adminserver.dao.QuotaSyncDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.lock.LockPathFactory;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.model.QuotaSyncInfo;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClient;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClientFactory;
import com.sequoiacm.cloud.adminserver.service.QuotaSyncService;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaUpdator;
import com.sequoiacm.infrastructure.discovery.ScmServiceInstance;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class QuotaSyncServiceImpl implements QuotaSyncService {
    private static final Logger logger = LoggerFactory.getLogger(QuotaSyncServiceImpl.class);

    @Autowired
    private QuotaSyncDao quotaSyncDao;

    @Autowired
    private QuotaSyncMsgSender quotaSyncMsgSender;

    @Autowired
    private QuotaHelper quotaHelper;

    @Autowired
    private ScmLockManager lockManager;

    @Autowired
    private LockPathFactory lockPathFactory;

    @Autowired
    private ScmConfClient confClient;

    private QuotaSyncConfig quotaSyncConfig;

    private ThreadPoolExecutor statisticsThreadPool;
    private long retryInterval;

    private ScmTimer scmTimer;

    public QuotaSyncServiceImpl(QuotaSyncConfig quotaSyncConfig) {
        this.quotaSyncConfig = quotaSyncConfig;
        statisticsThreadPool = new ThreadPoolExecutor(1, quotaSyncConfig.getMaxConcurrentCount(),
                60L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.AbortPolicy());
        scmTimer = setupRecoverSyncTask();
    }

    private ScmTimer setupRecoverSyncTask() {
        this.retryInterval = quotaSyncConfig.getRetryInterval() * 1000L;
        ScmTimer timer = ScmTimerFactory.createScmTimer();
        timer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    tryRecoverSync();
                }
                catch (Exception e) {
                    logger.warn("failed to recover sync quota", e);
                }
            }
        }, retryInterval, retryInterval);
        return timer;
    }

    @PreDestroy
    public void destroy() {
        if (statisticsThreadPool != null) {
            statisticsThreadPool.shutdown();
        }
        if (scmTimer != null) {
            scmTimer.cancel();
        }
    }

    @Override
    public void sync(String type, String name, boolean isFirstSync, Authentication auth)
            throws StatisticsException {
        logger.info("begin to sync quota:type={},name={},isFirstSync={}", type, name, isFirstSync);
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.ALL, "sync quota");
        startSyncTask(type, name, isFirstSync);
    }

    @Override
    public void cancelSync(String type, String name, Authentication auth)
            throws StatisticsException {
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.ALL,
                "cancel sync quota");
        try {
            QuotaSyncInfo syncInfo = quotaSyncDao.getQuotaSyncInfo(type, name, null);
            if (syncInfo == null) {
                throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                        "failed to cancel sync quota," + type + " quota is not exist:name=" + name);
            }
            if (!ScmQuotaSyncStatus.SYNCING.getName().equals(syncInfo.getStatus())) {
                throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                        "failed to cancel sync quota," + type + " quota status is not syncing:name="
                                + name + ",status=" + syncInfo.getStatus());
            }
            quotaSyncDao.cancelSync(type, name);
            quotaSyncMsgSender.sendCancelSyncMsgSilence(type, name, syncInfo.getSyncRoundNumber(),
                    syncInfo.getQuotaRoundNumber(), quotaHelper.getS3AndContentServerInstance());
        }
        catch (StatisticsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to cancel sync " + type + " quota:name=" + name, e);
        }
    }

    @EventListener
    public void handleRefreshScopeRefreshedEvent(RefreshScopeRefreshedEvent event) {
        if (this.retryInterval != quotaSyncConfig.getRetryInterval() * 1000L) {
            logger.info("update retry interval to {}", quotaSyncConfig.getRetryInterval() + "s");
            scmTimer.cancel();
            scmTimer = setupRecoverSyncTask();
        }
        if (statisticsThreadPool.getMaximumPoolSize() != quotaSyncConfig.getMaxConcurrentCount()) {
            logger.info("update max concurrent count to {}",
                    quotaSyncConfig.getMaxConcurrentCount());
            statisticsThreadPool.setMaximumPoolSize(quotaSyncConfig.getMaxConcurrentCount());
        }
    }

    private static void unLock(ScmLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }

    @Override
    public void startSyncTask(String type, String name, boolean isFirstSync)
            throws StatisticsException {
        SyncTaskContext context = new SyncTaskContext(type, name, isFirstSync);
        statisticsThreadPool.execute(new SyncTask(context));
        context.awaitStatisticsStart();
    }

    @Override
    public BSONObject getInnerDetail(String type, String name, Authentication auth)
            throws StatisticsException {
        quotaHelper.checkTypeAndName(type, name);
        quotaHelper.checkPriv(type, name, auth.getName(), ScmPrivilegeDefine.READ,
                "get inner quota detail");
        List<ScmServiceInstance> instances = quotaHelper.getS3AndContentServerInstance();
        try {
            BSONObject res = new BasicBSONObject();
            for (ScmServiceInstance instance : instances) {
                String url = instance.getHost() + ":" + instance.getPort();
                ContentServerClient client = ContentServerClientFactory
                        .getFeignClientByNodeUrl(url);
                res.put(url, client.getQuotaInnerDetail(type, name));
            }
            return res;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to get inner detail:" + e.getMessage(), e);
        }
    }

    private static class SyncTaskContext {
        private String type;
        private String name;
        private boolean isFirstSync;

        private volatile StatisticsException exception;

        private final CountDownLatch countDownLatch = new CountDownLatch(1);

        public SyncTaskContext(String type, String name, boolean isFirstSync) {
            this.type = type;
            this.name = name;
            this.isFirstSync = isFirstSync;
        }

        public void awaitStatisticsStart() throws StatisticsException {
            try {
                countDownLatch.await();
                if (exception != null) {
                    throw exception;
                }
            }
            catch (InterruptedException e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to await statistics start:type=" + type + ",name" + name
                                + ",isFirstSync=" + isFirstSync,
                        e);
            }
        }

        public void statisticsStart() throws StatisticsException {
            countDownLatch.countDown();
        }

        public void statisticsFailed(StatisticsException e) {
            exception = e;
            countDownLatch.countDown();
        }

    }

    private class SyncTask implements Runnable {

        private String type;
        private String name;
        private boolean isFirstSync;

        private SyncTaskContext context;

        public SyncTask(SyncTaskContext context) {
            this.type = context.type;
            this.name = context.name;
            this.isFirstSync = context.isFirstSync;
            this.context = context;
        }

        @Override
        public void run() {
            ScmLock lock = null;
            boolean statisticsStart = false;
            try {
                long count = quotaSyncDao.getSyncStatusCount(type);
                if (count >= quotaSyncConfig.getMaxConcurrentCount()) {
                    throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "failed to sync " + type
                                    + " quota,too many sync tasks are running:name=" + name
                                    + ",currentCount=" + count);
                }
                lock = tryAcquireSyncQuotaLock(type, name);
                if (lock == null) {
                    throw new StatisticsException(StatisticsError.QUOTA_SYNCING,
                            type + " quota is syncing, please try again later:name=" + name);
                }
                QuotaConfig quotaConfig = quotaHelper.getQuotaConfig(type, name);
                if (quotaConfig == null || !quotaConfig.isEnable()) {
                    throw new StatisticsException(StatisticsError.QUOTA_NOT_ENABLE,
                            "failed to sync quota," + type + " quota is not enable:name=" + name);
                }

                List<ScmServiceInstance> notifyInstances = quotaHelper
                        .getS3AndContentServerInstance();
                if (notifyInstances.isEmpty()) {
                    throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "no available s3-server or content-server");
                }

                QuotaSyncInfo oldSyncInfo = quotaSyncDao.getQuotaSyncInfo(type, name, null);
                QuotaSyncInfo newQuotaSyncInfo = createNewQuotaSyncInfo(type, name, isFirstSync,
                        quotaConfig.getQuotaRoundNumber(), oldSyncInfo);
                quotaSyncDao.updateQuotaSyncInfo(newQuotaSyncInfo);
                int syncRoundNumber = newQuotaSyncInfo.getSyncRoundNumber();
                int quotaRoundNumber = newQuotaSyncInfo.getQuotaRoundNumber();

                try {
                    determineAgreementTime(type, name, syncRoundNumber, quotaRoundNumber,
                            newQuotaSyncInfo.getExpireTime(), notifyInstances);
                    sendStatisticsRequest(type, name, syncRoundNumber);
                    context.statisticsStart();
                    statisticsStart = true;
                }
                catch (Exception e) {
                    handleSyncExceptionSilence(type, name, isFirstSync, syncRoundNumber,
                            quotaConfig.getQuotaRoundNumber(), notifyInstances, e);
                    throw e;
                }

                waitStatisticsFinish(type, name, syncRoundNumber, notifyInstances);
            }
            catch (StatisticsException e) {
                logger.error("failed ot sync {} quota:name={}", type, name, e);
                if (!statisticsStart) {
                    context.statisticsFailed(e);
                }

            }
            catch (Exception e) {
                logger.error("failed ot sync {} quota:name={}", type, name, e);
                if (!statisticsStart) {
                    context.statisticsFailed(new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "failed to sync " + type + " quota:name=" + name, e));
                }
            }
            finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    private void waitStatisticsFinish(String type, String name, int syncRoundNumber,
            List<ScmServiceInstance> notifyInstances) throws StatisticsException {
        try {
            // lastChangeTime 是本地记录的，用于判断统计信息是否有变化
            long lastChangeTime = System.currentTimeMillis();
            // lastUpdateTime 是从数据库中读取的，是上一次统计信息更新的时间
            long lastUpdateTime = System.currentTimeMillis();
            while (true) {
                QuotaSyncInfo quotaSyncInfo = quotaSyncDao.getQuotaSyncInfo(type, name, null);
                if (quotaSyncInfo == null) {
                    // 不存在说明桶已经被删除了
                    logger.warn(
                            "quotaSyncInfo is null, exit sync:type={},name={},syncRoundNumber={}",
                            type, name, syncRoundNumber);
                    break;
                }
                if (quotaSyncInfo.getSyncRoundNumber() != syncRoundNumber) {
                    throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "sync round number is not match:type=" + type + ",name=" + name
                                    + ",syncRoundNumber=" + syncRoundNumber + ",newRoundNumber="
                                    + quotaSyncInfo.getSyncRoundNumber());
                }

                // 统计取消
                if (!ScmQuotaSyncStatus.SYNCING.getName().equals(quotaSyncInfo.getStatus())) {
                    if (ScmQuotaSyncStatus.CANCELED.getName().equals(quotaSyncInfo.getStatus())) {
                        logger.info("quota sync canceled:type={},name={},syncRoundNumber={}", type,
                                name, syncRoundNumber);
                        quotaSyncMsgSender.sendCancelSyncMsgSilence(type, name, syncRoundNumber,
                                quotaSyncInfo.getQuotaRoundNumber(), notifyInstances);
                        break;
                    }
                    else {
                        throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                                "sync status is not syncing:type=" + type + ",name=" + name
                                        + ",syncRoundNumber=" + syncRoundNumber + ",status="
                                        + quotaSyncInfo.getStatus());
                    }
                }

                BSONObject progressDetail = quotaSyncInfo.getStatisticsDetail();
                if (progressDetail == null) {
                    throw new IllegalArgumentException("progressDetail is null");
                }

                Object processNode = progressDetail
                        .get(FieldName.QuotaStatisticsProgress.PROCESS_NODE);
                if (processNode == null) {
                    logger.warn("not statistics node:type={},name={},syncRoundNumber={}", type,
                            name, syncRoundNumber);
                    quotaSyncDao.recordError(type, name, syncRoundNumber, "no statistics node");
                    quotaSyncMsgSender.sendCancelSyncMsgSilence(type, name, syncRoundNumber,
                            quotaSyncInfo.getQuotaRoundNumber(), notifyInstances);
                    break;
                }

                String statisticsStatus = BsonUtils.getStringChecked(progressDetail,
                        FieldName.QuotaStatisticsProgress.STATUS);

                // 统计完成
                if (CommonDefine.QuotaStatisticsStatus.FINISH.equals(statisticsStatus)) {
                    logger.info("quota statistics completed:type={},name={},syncRoundNumber={}",
                            type, name, syncRoundNumber);
                    quotaSyncDao.recordCompleted(type, name, syncRoundNumber);
                    quotaSyncMsgSender.sendFinishSyncMsgSilence(type, name, syncRoundNumber,
                            quotaSyncInfo.getQuotaRoundNumber(), notifyInstances);
                    break;
                }

                // 统计出错
                if (CommonDefine.QuotaStatisticsStatus.FAILED.equals(statisticsStatus)) {
                    String errorMsg = BsonUtils.getString(progressDetail,
                            FieldName.QuotaStatisticsProgress.ERROR_MSG);
                    logger.warn(
                            "quota statistics error:type={},name={},syncRoundNumber={},errorMsg={}",
                            type, name, syncRoundNumber, errorMsg);
                    quotaSyncDao.recordError(type, name, syncRoundNumber, errorMsg);
                    quotaSyncMsgSender.sendCancelSyncMsgSilence(type, name, syncRoundNumber,
                            quotaSyncInfo.getQuotaRoundNumber(), notifyInstances);
                    break;
                }

                // 统计超时
                long updateTime = quotaSyncInfo.getUpdateTime();
                if (lastUpdateTime != updateTime) {
                    lastUpdateTime = updateTime;
                    lastChangeTime = System.currentTimeMillis();
                }
                long now = System.currentTimeMillis();
                if ((now - lastChangeTime) > quotaSyncConfig.getExpireTime() * 1000L) {
                    logger.warn("quota statistics timeout:type={},name={},syncRoundNumber={}", type,
                            name, syncRoundNumber);
                    quotaSyncDao.recordError(type, name, syncRoundNumber, "sync timeout");
                    quotaSyncMsgSender.sendCancelSyncMsgSilence(type, name, syncRoundNumber,
                            quotaSyncInfo.getQuotaRoundNumber(), notifyInstances);
                    break;
                }

                Thread.sleep(1000);
            }
        }
        catch (StatisticsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to wait quota statistics finish:type=" + type + ",name=" + name
                            + ",syncRoundNumber=" + syncRoundNumber,
                    e);
        }

    }

    private class WaitStatisticsFinishTask implements Runnable {
        private String type;
        private String name;
        private int syncRoundNumber;
        private List<ScmServiceInstance> notifyInstances;

        public WaitStatisticsFinishTask(String type, String name, int syncRoundNumber) {
            this.type = type;
            this.name = name;
            this.syncRoundNumber = syncRoundNumber;
            this.notifyInstances = quotaHelper.getS3AndContentServerInstance();
        }

        @Override
        public void run() {
            try {
                waitStatisticsFinish(type, name, syncRoundNumber, notifyInstances);
            }
            catch (Exception e) {
                logger.error("failed to wait statistics finish", e);
            }
        }
    }

    private void sendStatisticsRequest(String type, String name, int syncRoundNumber)
            throws Exception {
        ScmServiceInstance instance = quotaHelper.choseStatisticsInstanceRandom(type, name);
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByNodeUrl(instance.getHost() + ":" + instance.getPort());
        BSONObject syncProgressDetail = new BasicBSONObject();
        syncProgressDetail.put(FieldName.QuotaStatisticsProgress.STATUS,
                CommonDefine.QuotaStatisticsStatus.RUNNING);
        syncProgressDetail.put(FieldName.QuotaStatisticsProgress.ESTIMATED_TIME, -1L);
        syncProgressDetail.put(FieldName.QuotaStatisticsProgress.PROCESS_NODE,
                instance.getHost() + ":" + instance.getPort());
        quotaSyncDao.updateSyncStatisticsDetail(type, name, syncRoundNumber, syncProgressDetail);
        client.quotaStatistics(type, name, syncRoundNumber);
    }

    private void handleSyncExceptionSilence(String type, String name, boolean isFirstSync,
            int syncRoundNumber, int quotaRoundNumber, List<ScmServiceInstance> notifyInstances,
            Throwable e) {
        // 1. 登记错误信息
        try {
            quotaSyncDao.recordError(type, name, syncRoundNumber, e.getMessage());
        }
        catch (Exception ex) {
            logger.warn(
                    "failed to record error to quota statistics table:type={},name={},syncRoundNumber={}",
                    type, name, syncRoundNumber, ex);
        }

        // 2. 发送取消同步消息
        quotaSyncMsgSender.sendCancelSyncMsgSilence(type, name, syncRoundNumber, quotaRoundNumber,
                notifyInstances);

        // 3. 如果是第一次同步，将限额状态改为禁用
        if (isFirstSync) {
            try {
                QuotaUpdator quotaUpdator = new QuotaUpdator();
                quotaUpdator.setType(type);
                quotaUpdator.setName(name);
                quotaUpdator.setEnable(false);
                quotaUpdator.setMatcher(
                        new BasicBSONObject(FieldName.Quota.QUOTA_ROUND_NUMBER, quotaRoundNumber));
                confClient.updateConfig(ScmConfigNameDefine.QUOTA, quotaUpdator, false);
            }
            catch (Exception ex) {
                logger.warn("failed to disable quota", ex);
            }
        }
    }

    private void determineAgreementTime(String type, String name, int syncRoundNumber,
            int quotaRoundNumber, long expireTime, List<ScmServiceInstance> notifyInstances)
            throws StatisticsException {
        // 发送开始同步消息
        QuotaSyncMsgSender.StartSyncMsgResponse response = quotaSyncMsgSender.sendStartSyncMsg(type,
                name, syncRoundNumber, quotaRoundNumber, expireTime, notifyInstances);
        List<QuotaSyncMsgSender.Result> results = response.getResults();
        Collections.sort(results, new Comparator<QuotaSyncMsgSender.Result>() {
            @Override
            public int compare(QuotaSyncMsgSender.Result o1, QuotaSyncMsgSender.Result o2) {
                return Long.compare(o1.getNodeTime(), o2.getNodeTime());
            }
        });
        if (results.size() > 1) {
            long minNodeTime = results.get(0).getNodeTime();
            long maxNodeTime = results.get(results.size() - 1).getNodeTime();
            if ((maxNodeTime - minNodeTime) > quotaSyncConfig.getMaxTimeDiff()) {
                throw new StatisticsException(StatisticsError.QUOTA_SYNC_TIME_DIFF_TOO_LARGE,
                        "time diff between nodes is too large:diff=" + (maxNodeTime - minNodeTime)
                                + "ms, maxTimeNode=" + results.get(results.size() - 1).getNodeUrl()
                                + ", minTimeNode=" + results.get(0).getNodeUrl());
            }
        }

        // 发送设置协商时间消息
        long agreementTime = results.get(results.size() - 1).getNodeTime();
        long sleepTime = agreementTime - results.get(0).getNodeTime() + 1000;
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            }
            catch (InterruptedException e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to sleep before set agreement time", e);
            }
        }
        quotaSyncDao.updateAgreementTime(type, name, syncRoundNumber, agreementTime);
        quotaSyncMsgSender.sendSetAgreementTimeMsg(type, name, syncRoundNumber, quotaRoundNumber,
                agreementTime, results);
    }

    private ScmLock tryAcquireSyncQuotaLock(String type, String name) throws StatisticsException {
        try {
            ScmLockPath lockPath = lockPathFactory.quotaSyncPath(type, name);
            return lockManager.tryAcquiresLock(lockPath);
        }
        catch (ScmLockException e) {
            throw new StatisticsException(StatisticsError.LOCK_ERROR,
                    "failed to lock quota:type=" + type + ",name=" + name, e);
        }
    }

    private QuotaSyncInfo createNewQuotaSyncInfo(String type, String name, boolean isFirstSync,
            int quotaRoundNumber, QuotaSyncInfo oldQuotaSyncInfo) throws StatisticsException {
        QuotaSyncInfo quotaSyncInfo = new QuotaSyncInfo();
        quotaSyncInfo.setType(type);
        quotaSyncInfo.setName(name);
        quotaSyncInfo.setFirstSync(isFirstSync);
        quotaSyncInfo.setErrorMsg(null);
        if (oldQuotaSyncInfo != null) {
            quotaSyncInfo.setSyncRoundNumber(oldQuotaSyncInfo.getSyncRoundNumber() + 1);
        }
        else {
            quotaSyncInfo.setSyncRoundNumber(1);
        }
        quotaSyncInfo.setQuotaRoundNumber(quotaRoundNumber);
        quotaSyncInfo.setStatisticsSize(0L);
        quotaSyncInfo.setStatisticsObjects(0L);
        quotaSyncInfo.setBeginTime(System.currentTimeMillis());
        quotaSyncInfo.setEndTime(null);
        quotaSyncInfo.setStatus(ScmQuotaSyncStatus.SYNCING.getName());
        quotaSyncInfo.setAgreementTime(null);
        quotaSyncInfo.setStatisticsDetail(
                new BasicBSONObject(FieldName.QuotaStatisticsProgress.ESTIMATED_TIME, -1L));
        quotaSyncInfo.setExpireTime(quotaSyncConfig.getMaxTimeDiff() * 2);
        quotaSyncInfo.setExtraInfo(quotaHelper.generateExtraInfo(type, name));
        quotaSyncInfo.setUpdateTime(System.currentTimeMillis());
        return quotaSyncInfo;
    }

    /**
     * 本方法用于故障恢复，主要解决以下异常场景： 同步过程中，执行任务的 admin-server 异常退出，导致同步状态一直处于 syncing状态。
     */
    private void tryRecoverSync() throws Exception {
        logger.debug("scan syncing quota records");
        MetaCursor cursor = null;
        try {
            cursor = quotaSyncDao.querySyncStatusRecord();
            while (cursor.hasNext()) {
                BSONObject next = cursor.getNext();
                ScmLock lock = null;
                try {
                    String type = BsonUtils.getStringChecked(next, FieldName.QuotaSync.TYPE);
                    String name = BsonUtils.getStringChecked(next, FieldName.QuotaSync.NAME);
                    lock = tryAcquireSyncQuotaLock(type, name);
                    if (lock == null) {
                        continue;
                    }
                    QuotaSyncInfo statisticsInfo = quotaSyncDao.getQuotaSyncInfo(type, name, null);
                    if (statisticsInfo != null && statisticsInfo.getStatus()
                            .equals(ScmQuotaSyncStatus.SYNCING.getName())) {
                        logger.warn(
                                "quota sync may have been interrupted, try to recover sync:type={},name={}",
                                type, name);
                        statisticsThreadPool.execute(new WaitStatisticsFinishTask(type, name,
                                statisticsInfo.getSyncRoundNumber()));

                    }
                }
                catch (Exception e) {
                    logger.warn("failed to recover quota sync", e);
                }
                finally {
                    unLock(lock);
                }
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
