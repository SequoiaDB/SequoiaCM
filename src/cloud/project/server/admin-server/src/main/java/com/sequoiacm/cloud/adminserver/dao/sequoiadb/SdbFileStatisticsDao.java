package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataKey;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataQueryCondition;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SdbFileStatisticsDao implements com.sequoiacm.cloud.adminserver.dao.FileStatisticsDao {
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TIME = "time";
    private static final String FIELD_USER = "user";
    private static final String FIELD_WORKSPACE = "workspace";
    private static final String FIELD_REQ_COUNT = "request_count";
    private static final String FIELD_AVG_RESP_TIME = "avg_response_time";
    private static final String FIELD_AVG_TRAFFIC_SIZE = "avg_traffic_size";
    private static final String FIELD_MAX_RESP_TIME = "max_response_time";
    private static final String FIELD_MIN_RESP_TIME = "min_response_time";
    private static final String FIELD_FAIL_COUNT = "fail_count";

    private final SequoiadbMetaSource metasource;

    @Autowired
    public SdbFileStatisticsDao(SequoiadbMetaSource metasource) throws ScmMetasourceException {
        this.metasource = metasource;
        MetaAccessor accessor = metasource.getFileStatisticsAccessor();
        accessor.ensureTable();
        accessor.ensureIndex("idx_time", new BasicBSONObject(FIELD_TIME, 1), false);
    }

    @Override
    public FileStatisticsData getFileStatisticData(FileStatisticsDataKey key)
            throws StatisticsException {
        BSONObject matcher = keyToBson(key);
        MetaAccessor accessor = metasource.getFileStatisticsAccessor();
        BSONObject res = accessor.queryOne(matcher);
        if (res == null) {
            return null;
        }
        // maxResponseTime,minResponseTime,failCount since v3.1.3 SEQUOIACM-713
        long maxResponseTime = BsonUtils.getLongOrElse(res, FIELD_MAX_RESP_TIME, 0);
        long minResponseTime = BsonUtils.getLongOrElse(res, FIELD_MIN_RESP_TIME, 0);
        int failCount = BsonUtils.getIntegerOrElse(res, FIELD_FAIL_COUNT, 0);
        return new FileStatisticsData(BsonUtils.getIntegerChecked(res, FIELD_REQ_COUNT),
                BsonUtils.getLongChecked(res, FIELD_AVG_TRAFFIC_SIZE),
                BsonUtils.getLongChecked(res, FIELD_AVG_RESP_TIME), maxResponseTime,
                minResponseTime, failCount);
    }

    @Override
    public void saveFileStatisticData(FileStatisticsDataKey keyInfo, FileStatisticsData data)
            throws StatisticsException {
        MetaAccessor accessor = metasource.getFileStatisticsAccessor();
        BSONObject keyBson = keyToBson(keyInfo);
        BasicBSONObject record = new BasicBSONObject();
        record.putAll(keyBson);
        record.put(FIELD_AVG_TRAFFIC_SIZE, data.getAvgTrafficSize());
        record.put(FIELD_AVG_RESP_TIME, data.getAvgResponseTime());
        record.put(FIELD_MAX_RESP_TIME, data.getMaxResponseTime());
        record.put(FIELD_MIN_RESP_TIME, data.getMinResponseTime());
        record.put(FIELD_FAIL_COUNT, data.getFailCount());
        record.put(FIELD_REQ_COUNT, data.getRequestCount());
        accessor.upsert(new BasicBSONObject("$set", record), keyBson);
    }

    @Override
    public FileStatisticsData getFileStatisticData(String fileStatisticType,
            FileStatisticsDataQueryCondition condition) throws StatisticsException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FIELD_TYPE, fileStatisticType);

        BSONObject gteTime = new BasicBSONObject("$gte", condition.getBegin());
        BSONObject ltTime = new BasicBSONObject("$lt", condition.getEnd());
        BasicBSONList andArr = new BasicBSONList();
        andArr.add(new BasicBSONObject(FIELD_TIME, gteTime));
        andArr.add(new BasicBSONObject(FIELD_TIME, ltTime));
        matcher.put("$and", andArr);

        if (condition.getUser() != null) {
            matcher.put(FIELD_USER, condition.getUser());
        }
        if (condition.getWorkspace() != null) {
            matcher.put(FIELD_WORKSPACE, condition.getWorkspace());
        }

        int requestCount = 0;
        int failCount = 0;
        long totalRespTime = 0;
        long totalTrafficSize = 0;
        long maxResponseTime = Long.MIN_VALUE;
        long minResponseTime = Long.MAX_VALUE;
        MetaAccessor accessor = metasource.getFileStatisticsAccessor();
        MetaCursor cursor = accessor.query(matcher, null, null);
        try {
            while (cursor.hasNext()) {
                BSONObject record = cursor.getNext();
                FileStatisticsData statisticsData = new FileStatisticsData(
                        BsonUtils.getIntegerChecked(record, FIELD_REQ_COUNT),
                        BsonUtils.getLongChecked(record, FIELD_AVG_TRAFFIC_SIZE),
                        BsonUtils.getLongChecked(record, FIELD_AVG_RESP_TIME));
                statisticsData
                        .setFailCount(BsonUtils.getIntegerOrElse(record, FIELD_FAIL_COUNT, 0));
                failCount += statisticsData.getFailCount();
                requestCount += statisticsData.getRequestCount();
                long successCount = statisticsData.getRequestCount()
                        - statisticsData.getFailCount();
                if (successCount > 0) {
                    totalRespTime += (successCount * statisticsData.getAvgResponseTime());
                    totalTrafficSize += (successCount * statisticsData.getAvgTrafficSize());
                    long maxResTime = BsonUtils.getLongOrElse(record, FIELD_MAX_RESP_TIME,
                            Long.MIN_VALUE);
                    maxResponseTime = Math.max(maxResponseTime, maxResTime);
                    long minResTime = BsonUtils.getLongOrElse(record, FIELD_MIN_RESP_TIME,
                            Long.MAX_VALUE);
                    minResponseTime = Math.min(minResponseTime, minResTime);
                }
            }
        }
        finally {
            cursor.close();
        }
        if (requestCount <= 0) {
            return new FileStatisticsData();
        }
        int totalSuccessCount = requestCount - failCount;
        if (maxResponseTime == Long.MIN_VALUE) {
            maxResponseTime = 0;
        }
        if (minResponseTime == Long.MAX_VALUE) {
            minResponseTime = 0;
        }
        long avgTrafficSize = 0;
        long avgResponseTime = 0;
        if (totalSuccessCount > 0) {
            avgTrafficSize = totalTrafficSize / totalSuccessCount;
            avgResponseTime = totalRespTime / totalSuccessCount;
        }
        return new FileStatisticsData(requestCount, avgTrafficSize, avgResponseTime,
                maxResponseTime, minResponseTime, failCount);
    }

    private BSONObject keyToBson(FileStatisticsDataKey key) {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FIELD_TYPE, key.getType());
        matcher.put(FIELD_TIME, key.getTime());
        matcher.put(FIELD_USER, key.getUser());
        matcher.put(FIELD_WORKSPACE, key.getWorkspace());
        return matcher;
    }
}
