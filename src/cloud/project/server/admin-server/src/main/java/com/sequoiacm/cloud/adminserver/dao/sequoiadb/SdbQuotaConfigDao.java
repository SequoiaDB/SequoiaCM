package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import com.sequoiacm.cloud.adminserver.dao.QuotaConfigDao;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.QuotaConfigDetail;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository
public class SdbQuotaConfigDao implements QuotaConfigDao {

    @Autowired
    private SequoiadbMetaSource metasource;

    @PostConstruct
    public void init() throws ScmMetasourceException {
        MetaAccessor accessor = metasource.getQuotaConfigAccessor();
        accessor.ensureTable();
        BasicBSONObject indexBson = new BasicBSONObject();
        indexBson.put(FieldName.Quota.TYPE, 1);
        indexBson.put(FieldName.Quota.NAME, 1);
        accessor.ensureIndex("idx_type_name", indexBson, true);
    }

    @Override
    public QuotaConfigDetail getQuotaConfigInfo(String type, String name)
            throws StatisticsException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Quota.TYPE, type);
        matcher.put(FieldName.Quota.NAME, name);
        BSONObject bsonObject = metasource.getQuotaConfigAccessor().queryOne(matcher);
        if (bsonObject != null) {
            return new QuotaConfigDetail(bsonObject);
        }
        return null;
    }

    @Override
    public void updateQuotaUsedInfo(String type, String name, Long usedObjects, Long usedSizeBytes)
            throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Quota.TYPE, type);
        matcher.put(FieldName.Quota.NAME, name);
        BSONObject updator = new BasicBSONObject();
        if (usedObjects != null) {
            updator.put(FieldName.Quota.USED_OBJECTS, usedObjects);
        }
        if (usedSizeBytes != null) {
            updator.put(FieldName.Quota.USED_SIZE, usedSizeBytes);
        }
        metasource.getQuotaConfigAccessor().upsert(new BasicBSONObject("$set", updator), matcher);
    }
}
