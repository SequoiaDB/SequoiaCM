package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.cloud.adminserver.common.FieldName;
import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.dao.StatisticsDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.BsonTranslator;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;
import com.sequoiacm.cloud.adminserver.model.TrafficInfo;

@Repository
public class SdbStatisticsDao implements StatisticsDao {

    @Autowired
    private SequoiadbMetaSource metasource;

    @Override
    public TrafficInfo queryLastTrafficRecord(String type, String workspace)
            throws StatisticsException {
        MetaAccessor trafficAccessor = metasource.getTrafficAccessor();
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Traffic.FIELD_TYPE, type);
        matcher.put(FieldName.Traffic.FIELD_WORKSPACE_NAME, workspace);
        BasicBSONObject order = new BasicBSONObject(FieldName.Traffic.FIELD_RECORD_TIME, -1);

        BSONObject ret = SequoiadbHelper.queryOne(trafficAccessor, matcher, order);
        if (ret != null) {
            return BsonTranslator.Traffic.fromBSONObject(ret);
        }
        else {
            return null;
        }
    }
    
    @Override
    public FileDeltaInfo queryLastFileDeltaRecord(String workspace)
            throws StatisticsException {
        MetaAccessor fileDeltaAccessor = metasource.getFileDeltaAccessor();
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FileDelta.FIELD_WORKSPACE_NAME, workspace);
        BasicBSONObject order = new BasicBSONObject(FieldName.FileDelta.FIELD_RECORD_TIME, -1);

        BSONObject ret = SequoiadbHelper.queryOne(fileDeltaAccessor, matcher, order);
        if (ret != null) {
            return BsonTranslator.FileDelta.fromBSONObject(ret);
        }
        else {
            return null;
        }
    }

    @Override
    public void upsertTraffic(String type, String workspace, long recordTime, long newTraffic)
            throws StatisticsException {
        MetaAccessor trafficAccessor = metasource.getTrafficAccessor();
        BasicBSONObject upsertor = new BasicBSONObject(SequoiadbHelper.DOLLAR_SET,
                new BasicBSONObject(FieldName.Traffic.FIELD_TRAFFIC, newTraffic));
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Traffic.FIELD_TYPE, type);
        matcher.put(FieldName.Traffic.FIELD_WORKSPACE_NAME, workspace);
        matcher.put(FieldName.Traffic.FIELD_RECORD_TIME, recordTime);
        trafficAccessor.upsert(upsertor, matcher);
    }

    @Override
    public void upsertFileDelta(String workspace, long recordTime, long newCount, long newSize)
            throws StatisticsException {
        MetaAccessor fileDeltaAccessor = metasource.getFileDeltaAccessor();
        BasicBSONObject set = new BasicBSONObject();
        set.put(FieldName.FileDelta.FIELD_COUNT_DELTA, newCount);
        set.put(FieldName.FileDelta.FIELD_SIZE_DELTA, newSize);
        BasicBSONObject upsertor = new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, set);
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FileDelta.FIELD_WORKSPACE_NAME, workspace);
        matcher.put(FieldName.FileDelta.FIELD_RECORD_TIME, recordTime);
        fileDeltaAccessor.upsert(upsertor, matcher);
    }
    
    @Override
    public MetaCursor getTrafficList(BSONObject filter) throws StatisticsException {
        MetaAccessor trafficAccessor = metasource.getTrafficAccessor();
        BasicBSONObject order = new BasicBSONObject(FieldName.Traffic.FIELD_RECORD_TIME, 1);
        return trafficAccessor.query(filter, null, order);
    }
    
    @Override
    public MetaCursor getFileDeltaList(BSONObject filter) throws StatisticsException {
        MetaAccessor fileDeltaAccessor = metasource.getFileDeltaAccessor();
        BasicBSONObject order = new BasicBSONObject(FieldName.FileDelta.FIELD_RECORD_TIME, 1);
        return fileDeltaAccessor.query(filter, null, order);
    }
}
