package com.sequoiacm.metasource.sequoiadb.accessor;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.MetaSpaceRecyclingLogAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.module.ScmRecyclingLog;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class SdbSpaceRecyclingLogAccessor extends SdbMetaAccessor
        implements MetaSpaceRecyclingLogAccessor {

    public SdbSpaceRecyclingLogAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public void delete(BSONObject matcher) throws SdbMetasourceException {
        delete(matcher, null);
    }

    @Override
    public ScmRecyclingLog queryOneRecyclingLog(BSONObject matcher) throws ScmMetasourceException {
        BSONObject bsonObject = super.queryOne(matcher, null, null);
        if (bsonObject == null) {
            return null;
        }
        ScmRecyclingLog scmRecyclingLog = new ScmRecyclingLog(
                BsonUtils.getInteger(bsonObject, FieldName.FIELD_CLRECYCLE_SITE_ID),
                BsonUtils.getString(bsonObject, FieldName.FIELD_CLRECYCLE_DATA_SOURCE_TYPE),
                BsonUtils.getBSON(bsonObject, FieldName.FIELD_CLRECYCLE_LOG_INFO),
                BsonUtils.getString(bsonObject, FieldName.FIELD_CLRECYCLE_TIME));
        return scmRecyclingLog;
    }

    @Override
    public void insertRecyclingLog(ScmRecyclingLog scmRecyclingLog) throws ScmMetasourceException {
        BSONObject record = new BasicBSONObject();
        record.put(FieldName.FIELD_CLRECYCLE_SITE_ID, scmRecyclingLog.getSiteId());
        record.put(FieldName.FIELD_CLRECYCLE_DATA_SOURCE_TYPE, scmRecyclingLog.getDataSourceType());
        record.put(FieldName.FIELD_CLRECYCLE_LOG_INFO, scmRecyclingLog.getLogInfo());
        record.put(FieldName.FIELD_CLRECYCLE_TIME, scmRecyclingLog.getTime());
        super.insert(record);
    }
}
