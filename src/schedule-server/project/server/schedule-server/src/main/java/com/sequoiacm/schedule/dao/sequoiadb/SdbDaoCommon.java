package com.sequoiacm.schedule.dao.sequoiadb;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

public class SdbDaoCommon {
    private static final Logger logger = LoggerFactory.getLogger(SdbDaoCommon.class);

    public static void closeCursor(DBCursor cursor) {
        if (null != cursor) {
            try {
                cursor.close();
            }
            catch (Exception e) {
                logger.warn("close cursor failed", e);
            }
        }
    }

    public static ScmBSONObjectCursor query(SdbDataSourceWrapper datasource, String csName,
            String clName, BSONObject matcher) throws Exception {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cursor = cl.query(matcher, null, null, null);

            return new SdbBSONObjectCursor(datasource, sdb, cursor);
        }
        catch (Exception e) {
            SdbDaoCommon.closeCursor(cursor);
            datasource.releaseConnection(sdb);
            logger.error("query schedule failed[cs={},cl={}]:info={}", csName, clName, matcher);
            throw e;
        }
    }

    public static ScmBSONObjectCursor query(SdbDataSourceWrapper datasource, String csName,
            String clName, BSONObject matcher, BSONObject orderBy, long skip, long limit)
            throws Exception {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cursor = cl.query(matcher, null, orderBy, null, skip, limit);
            return new SdbBSONObjectCursor(datasource, sdb, cursor);
        }
        catch (Exception e) {
            SdbDaoCommon.closeCursor(cursor);
            datasource.releaseConnection(sdb);
            logger.error("query schedule failed[cs={},cl={},orderBy={},skip={},limit={}]:info={}",
                    csName, clName, orderBy, skip, limit, matcher);
            throw e;
        }
    }
}
