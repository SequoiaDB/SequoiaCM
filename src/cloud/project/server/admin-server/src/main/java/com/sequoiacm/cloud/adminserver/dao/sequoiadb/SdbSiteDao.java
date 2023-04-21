package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.cloud.adminserver.dao.SiteDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;

@Repository
public class SdbSiteDao implements SiteDao {

    @Autowired
    private SequoiadbMetaSource metasource;
    
    @Override
    public MetaCursor query(BSONObject matcher) throws StatisticsException {
        MetaAccessor siteAccessor = metasource.getSiteAccessor();
        return siteAccessor.query(matcher, null, null);
    }

    @Override
    public int getRootSiteId() throws StatisticsException {
        MetaAccessor siteAccessor = metasource.getSiteAccessor();
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLSITE_MAINFLAG, true);
        BSONObject bsonObject = siteAccessor.queryOne(matcher);
        if (bsonObject == null) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR, "root site not exists");
        }
        return BsonUtils.getNumberChecked(bsonObject, FieldName.FIELD_CLSITE_ID).intValue();
    }
}
