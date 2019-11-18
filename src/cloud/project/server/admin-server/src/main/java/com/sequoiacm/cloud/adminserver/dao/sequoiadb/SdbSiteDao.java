package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import org.bson.BSONObject;
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
        MetaAccessor wsAccessor = metasource.getWorkspaceAccessor();
        return wsAccessor.query(matcher, null, null);
    }
}
