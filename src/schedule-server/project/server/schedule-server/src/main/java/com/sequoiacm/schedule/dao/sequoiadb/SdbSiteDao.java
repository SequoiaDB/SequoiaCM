package com.sequoiacm.schedule.dao.sequoiadb;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.dao.SiteDao;
import com.sequoiacm.schedule.entity.ConfigEntityTranslator;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.entity.SiteEntity;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

@Repository("SiteDao")
public class SdbSiteDao implements SiteDao {
    private static final Logger logger = LoggerFactory.getLogger(SdbSiteDao.class);
    private SdbDataSourceWrapper datasource;
    private String csName = "SCMSYSTEM";
    private String clName = "SITE";

    @Autowired
    public SdbSiteDao(SdbDataSourceWrapper datasource) {
        this.datasource = datasource;
    }

    @Override
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher);
    }

    @Override
    public SiteEntity queryOne(String siteName) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Site.FIELD_NAME, siteName);
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, null, null, 0);
            if (null == result) {
                return null;
            }
            return ConfigEntityTranslator.Site.fromBSONObject(result);
        }
        catch (Exception e) {
            logger.error("query site failed[cs={},cl={}]:matcher={}", csName, clName, matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

}
