package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.dao.BreakpointFileStatisticsDao;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.BreakpointFileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.BreakpointFileStatisticsDataKey;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SdbBreakpointFileStatisticsDao implements BreakpointFileStatisticsDao {

    private static final String FIELD_CREATE_TIME = "create_time";
    private static final String FIELD_TOTAL_UPLOAD_TIME = "total_upload_time";
    private static final String FIELD_WORKSPACE_NAME = "workspace_name";
    private static final String FIELD_FILE_NAME = "file_name";

    private final SequoiadbMetaSource metasource;

    @Autowired
    public SdbBreakpointFileStatisticsDao(SequoiadbMetaSource metasource)
            throws ScmMetasourceException {
        this.metasource = metasource;
        MetaAccessor accessor = metasource.getBreakpointFileStatisticsAccessor();
        accessor.ensureTable();
        BasicBSONObject indexBson = new BasicBSONObject();
        indexBson.put(FIELD_CREATE_TIME, 1);
        indexBson.put(FIELD_FILE_NAME, 1);
        indexBson.put(FIELD_WORKSPACE_NAME, 1);
        accessor.ensureIndex("idx_breakpoint_file", indexBson, true);
    }

    @Override
    public long getTotalUploadTime(String fileName, String workspace, long createTime)
            throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FIELD_FILE_NAME, fileName);
        matcher.put(FIELD_WORKSPACE_NAME, workspace);
        matcher.put(FIELD_CREATE_TIME, createTime);
        MetaAccessor accessor = metasource.getBreakpointFileStatisticsAccessor();
        BSONObject result = accessor.queryOne(matcher);
        if (result == null) {
            return 0L;
        }
        return BsonUtils.getLongChecked(result, FIELD_TOTAL_UPLOAD_TIME);
    }

    @Override
    public void saveBreakpointFileRecord(BreakpointFileStatisticsData record)
            throws ScmMetasourceException {
        MetaAccessor accessor = metasource.getBreakpointFileStatisticsAccessor();
        BSONObject bson = keyToBson(record.getDataKey());
        bson.put(FIELD_TOTAL_UPLOAD_TIME, record.getTotalUploadTime());
        accessor.insert(bson);
    }

    @Override
    public void incrTotalUploadTime(BreakpointFileStatisticsDataKey key, long uploadTime)
            throws ScmMetasourceException {
        MetaAccessor accessor = metasource.getBreakpointFileStatisticsAccessor();
        BSONObject matcher = keyToBson(key);
        BSONObject incrModifier = new BasicBSONObject(FIELD_TOTAL_UPLOAD_TIME, uploadTime);
        accessor.upsert(
                new BasicBSONObject(StatisticsDefine.Modifier.SEQUOIADB_MODIFIER_INC, incrModifier),
                matcher);
    }

    @Override
    public void deleteBreakpointFileRecord(String fileName, String workspace, long createTime)
            throws ScmMetasourceException {
        MetaAccessor accessor = metasource.getBreakpointFileStatisticsAccessor();
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FIELD_FILE_NAME, fileName);
        matcher.put(FIELD_WORKSPACE_NAME, workspace);
        matcher.put(FIELD_CREATE_TIME, createTime);
        accessor.delete(matcher);
    }

    @Override
    public void clearRecords(long maxStayDay) throws ScmMetasourceException {
        MetaAccessor accessor = metasource.getBreakpointFileStatisticsAccessor();
        BSONObject matcher = new BasicBSONObject();
        long minTime = System.currentTimeMillis() - 24L * 60 * 60 * 1000 * maxStayDay;
        matcher.put(FIELD_CREATE_TIME,
                new BasicBSONObject(StatisticsDefine.Query.SEQUOIADB_MATCHER_LT, minTime));
        accessor.delete(matcher);

    }

    @Override
    public boolean exist(BreakpointFileStatisticsDataKey key) throws ScmMetasourceException {
        MetaAccessor accessor = metasource.getBreakpointFileStatisticsAccessor();
        BSONObject matcher = keyToBson(key);
        return accessor.getCount(matcher) > 0;
    }

    private BSONObject keyToBson(BreakpointFileStatisticsDataKey key) {
        BSONObject bson = new BasicBSONObject();
        bson.put(FIELD_FILE_NAME, key.getFileName());
        bson.put(FIELD_WORKSPACE_NAME, key.getWorkspaceName());
        bson.put(FIELD_CREATE_TIME, key.getCreateTime());
        return bson;
    }
}
