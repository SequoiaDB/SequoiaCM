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
        return new FileStatisticsData(BsonUtils.getIntegerChecked(res, FIELD_REQ_COUNT),
                BsonUtils.getLongChecked(res, FIELD_AVG_TRAFFIC_SIZE),
                BsonUtils.getLongChecked(res, FIELD_AVG_RESP_TIME));
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
        long totalRespTime = 0;
        long totalTrafficSize = 0;
        MetaAccessor accessor = metasource.getFileStatisticsAccessor();
        MetaCursor cursor = accessor.query(matcher, null, null);
        try {
            while (cursor.hasNext()) {
                BSONObject record = cursor.getNext();
                FileStatisticsData statisticsData = new FileStatisticsData(
                        BsonUtils.getIntegerChecked(record, FIELD_REQ_COUNT),
                        BsonUtils.getLongChecked(record, FIELD_AVG_TRAFFIC_SIZE),
                        BsonUtils.getLongChecked(record, FIELD_AVG_RESP_TIME));
                requestCount += statisticsData.getRequestCount();
                totalRespTime += (statisticsData.getRequestCount()
                        * statisticsData.getAvgResponseTime());
                totalTrafficSize += (statisticsData.getRequestCount()
                        * statisticsData.getAvgTrafficSize());
            }
        }
        finally {
            cursor.close();
        }
        if (requestCount <= 0) {
            return new FileStatisticsData();
        }
        return new FileStatisticsData(requestCount, totalTrafficSize / requestCount,
                totalRespTime / requestCount);
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
