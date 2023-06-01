package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import com.sequoiacm.cloud.adminserver.model.ObjectDeltaInfo;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.dao.StatisticsDao;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.BsonTranslator;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;
import com.sequoiacm.cloud.adminserver.model.TrafficInfo;

@Repository
public class SdbStatisticsDao implements StatisticsDao {
    private SequoiadbMetaSource metasource;

    @Autowired
    public SdbStatisticsDao(SequoiadbMetaSource metasource) throws ScmMetasourceException {
        this.metasource = metasource;
        ensureTable();
        ensureIndexes();
    }

    private void ensureTable() throws ScmMetasourceException {
        metasource.getObjectDeltaAccessor().ensureTable();
    }

    private void ensureIndexes() throws ScmMetasourceException {
        ensureTrafficIdIndex();
        ensureFileDeltaIdIndex();
        ensureObjectDeltaIndex();
    }

    private void ensureTrafficIdIndex() throws ScmMetasourceException {
        MetaAccessor trafficAccessor = metasource.getTrafficAccessor();
        BSONObject def = new BasicBSONObject(FieldName.Traffic.FIELD_RECORD_TIME, 1);
        trafficAccessor.ensureIndex("record_time_index", def, false);
    }

    private void ensureFileDeltaIdIndex() throws ScmMetasourceException {
        MetaAccessor fileDeltaAccessor = metasource.getFileDeltaAccessor();
        BSONObject def = new BasicBSONObject(FieldName.FileDelta.FIELD_RECORD_TIME, 1);
        fileDeltaAccessor.ensureIndex("record_time_index", def, false);
    }

    private void ensureObjectDeltaIndex() throws ScmMetasourceException {
        MetaAccessor objectDeltaAccessor = metasource.getObjectDeltaAccessor();
        BSONObject def = new BasicBSONObject();
        def.put(FieldName.ObjectDelta.FIELD_BUCKET_NAME, 1);
        def.put(FieldName.ObjectDelta.FIELD_RECORD_TIME, 1);
        objectDeltaAccessor.ensureIndex("record_bucket_time_index", def, false);
    }

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
    public FileDeltaInfo queryLastFileDeltaRecord(String workspace) throws StatisticsException {
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
    public ObjectDeltaInfo queryLastObjectDeltaRecord(String bucketName)
            throws StatisticsException {
        MetaAccessor objectDeltaAccessor = metasource.getObjectDeltaAccessor();
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.ObjectDelta.FIELD_BUCKET_NAME, bucketName);
        BasicBSONObject order = new BasicBSONObject(FieldName.ObjectDelta.FIELD_RECORD_TIME, -1);
        BSONObject ret = SequoiadbHelper.queryOne(objectDeltaAccessor, matcher, order);
        if (ret != null) {
            return BsonTranslator.ObjectDelta.fromBSONObject(ret);
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
        set.put(FieldName.FileDelta.FIELD_UPDATE_TIME, System.currentTimeMillis());
        BasicBSONObject upsertor = new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, set);
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FileDelta.FIELD_WORKSPACE_NAME, workspace);
        matcher.put(FieldName.FileDelta.FIELD_RECORD_TIME, recordTime);
        fileDeltaAccessor.upsert(upsertor, matcher);
    }

    @Override
    public void upsertObjectDelta(String bucketName, long recordTime, long newCount, long newSize)
            throws ScmMetasourceException {
        MetaAccessor objectDeltaAccessor = metasource.getObjectDeltaAccessor();
        BasicBSONObject set = new BasicBSONObject();
        set.put(FieldName.ObjectDelta.FIELD_COUNT_DELTA, newCount);
        set.put(FieldName.ObjectDelta.FIELD_SIZE_DELTA, newSize);
        set.put(FieldName.ObjectDelta.FIELD_UPDATE_TIME, System.currentTimeMillis());
        BasicBSONObject upsertor = new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, set);
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.ObjectDelta.FIELD_BUCKET_NAME, bucketName);
        matcher.put(FieldName.ObjectDelta.FIELD_RECORD_TIME, recordTime);
        objectDeltaAccessor.upsert(upsertor, matcher);
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

    @Override
    public MetaCursor getObjectDeltaList(BSONObject filter) throws ScmMetasourceException {
        MetaAccessor objectDeltaAccessor = metasource.getObjectDeltaAccessor();
        BasicBSONObject order = new BasicBSONObject(FieldName.ObjectDelta.FIELD_RECORD_TIME, 1);
        return objectDeltaAccessor.query(filter, null, order);
    }
}
