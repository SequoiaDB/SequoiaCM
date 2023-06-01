package com.sequoiacm.cloud.adminserver.core;

import com.sequoiacm.cloud.adminserver.common.CommonUtils;
import com.sequoiacm.cloud.adminserver.common.FieldName;
import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;
import com.sequoiacm.cloud.adminserver.model.ObjectDeltaInfo;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClient;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClientFactory;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatisticsObjectDelta {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalFileDelta.class);

    private static final long MIN_STATISTICS_INTERVAL = 1000 * 60 * 30L; // 30 minutes

    public void doStatistics(boolean needBacktrace) throws StatisticsException {
        List<String> buckets = StatisticsServer.getInstance().getBuckets();
        _execute(buckets, false, needBacktrace, false);
    }

    public void refresh(String... buckets) throws StatisticsException {
        _execute(Arrays.asList(buckets), true, true, true);
    }

    private void _execute(List<String> buckets, boolean isRefresh, boolean needBacktrace,
            boolean forceUpdate) throws StatisticsException {
        if (buckets == null || buckets.isEmpty()) {
            logger.warn("statistical traffic: no available buckets");
            return;
        }

        Map<Integer, List<ContentServerInfo>> allServersMap = CommonUtils.getAllServersMap();

        for (String bucketName : buckets) {
            List<ContentServerInfo> conformServers = getConformServers(bucketName, allServersMap);
            if (forceUpdate) {
                recordBucketDelta(bucketName, isRefresh, needBacktrace, conformServers);
            }
            else {
                ScmLock lock = null;
                try {
                    StatisticsServer statisticsServer = StatisticsServer.getInstance();
                    ScmLockPath lockPath = statisticsServer.getLockPathFactory()
                            .objectDeltaStatisticsLock(bucketName);
                    lock = statisticsServer.getLockManager().acquiresLock(lockPath);
                    ObjectDeltaInfo objectDeltaRecord = statisticsServer
                            .getLastObjectDeltaRecord(bucketName);
                    if (objectDeltaRecord != null && (System.currentTimeMillis()
                            - objectDeltaRecord.getUpdateTime()) < MIN_STATISTICS_INTERVAL) {
                        logger.info(
                                "statistics have been performed recently, skip it:bucket={},lastUpdateTime={}",
                                bucketName, objectDeltaRecord.getUpdateTime());
                    }
                    else {
                        recordBucketDelta(bucketName, isRefresh, needBacktrace, conformServers);
                    }

                }
                catch (ScmLockException e) {
                    throw new StatisticsException(StatisticsError.LOCK_ERROR,
                            "failed to acquire object statistics lock", e);
                }
                finally {
                    if (lock != null) {
                        lock.unlock();
                    }
                }

            }

        }
    }

    private void recordBucketDelta(String bucketName, boolean isRefresh, boolean needBacktrace,
            List<ContentServerInfo> tmpServers) throws StatisticsException {
        StatisticsServer statisticsServer = StatisticsServer.getInstance();
        // ensure date sequence and do not repeat
        Set<Date> dateSet = new LinkedHashSet<>();
        Date today = CommonUtils.getToday();
        Date yesterday = CommonUtils.getYesterday(today);

        // go back to historical records to fill in missing statistics.
        // for example, statistics service is down for a few days, it needs
        // to be backtracked when it starts again.
        if (needBacktrace) {
            ObjectDeltaInfo record = statisticsServer.getLastObjectDeltaRecord(bucketName);
            if (record != null) {
                dateSet = CommonUtils.getDateRange(new Date(record.getRecordTime()), yesterday);
            }
        }

        dateSet.add(yesterday);

        if (isRefresh) {
            dateSet.add(today);
        }

        BasicBSONObject and = new BasicBSONObject();
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.File.FIELD_CREATE_TIME, and);

        for (Date date : dateSet) {
            and.put(SequoiadbHelper.DOLLAR_GTE, date.getTime());
            and.put(SequoiadbHelper.DOLLAR_LT, CommonUtils.getTomorrow(date).getTime());

            ObjectDeltaInfo objectDeltaInfo = getObjectDeltaInfoRandomly(tmpServers, bucketName,
                    matcher);
            long count = objectDeltaInfo.getCountDelta();
            long size = objectDeltaInfo.getSizeDelta();
            statisticsServer.upsertObjectDeltaRecord(bucketName, date.getTime(), count, size);
            logger.debug("update file delta record success:workspace={},time={},count={},size={}",
                    bucketName, date.getTime(), count, size);
        }
    }

    private ObjectDeltaInfo getObjectDeltaInfo(ContentServerInfo contentServer, String bucketName,
            BSONObject condition) throws Exception {
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByNodeUrl(contentServer.getNodeUrl());
        BSONObject res = client.getObjectDeltaKeepAlive(bucketName, condition);
        ScmFeignExceptionUtils.handleException(res);

        ObjectDeltaInfo objectDeltaInfo = new ObjectDeltaInfo();
        objectDeltaInfo.setCountDelta(BsonUtils
                .getNumberChecked(res, FieldName.ObjectDelta.FIELD_COUNT_DELTA).longValue());
        objectDeltaInfo.setSizeDelta(BsonUtils
                .getNumberChecked(res, FieldName.ObjectDelta.FIELD_SIZE_DELTA).longValue());
        objectDeltaInfo.setBucketName(
                BsonUtils.getStringChecked(res, FieldName.ObjectDelta.FIELD_BUCKET_NAME));
        logger.debug(
                "access remote success:{},bucketName={},filter={},count_delta={},size_delta={}",
                contentServer.getNodeUrl(), bucketName, condition, objectDeltaInfo.getCountDelta(),
                objectDeltaInfo.getSizeDelta());
        return objectDeltaInfo;
    }

    private ObjectDeltaInfo getObjectDeltaInfoRandomly(List<ContentServerInfo> serverInfos,
            String bucketName, BSONObject condition) throws StatisticsException {
        ContentServerInfo randomServer = CommonUtils.getRandomElement(serverInfos);
        while (randomServer != null) {
            try {
                return getObjectDeltaInfo(randomServer, bucketName, condition);
            }
            catch (Exception e) {
                // remove exception server
                serverInfos.remove(randomServer);
                logger.warn("access remote node failed:remote={}", randomServer.getNodeUrl(), e);
            }

            randomServer = CommonUtils.getRandomElement(serverInfos);
        }

        throw new StatisticsException(StatisticsError.INTERNAL_ERROR, "no accessible remote node");
    }

    private List<ContentServerInfo> getConformServers(String bucketName,
            Map<Integer, List<ContentServerInfo>> allServerMap) throws StatisticsException {
        StatisticsServer statisticsServer = StatisticsServer.getInstance();
        BucketConfig bucket = statisticsServer.getBucket(bucketName);
        WorkspaceInfo workspaceInfo = statisticsServer.getWorkspaceChecked(bucket.getWorkspace());
        return CommonUtils.getConformServers(workspaceInfo.getSiteList(), allServerMap);
    }
}
