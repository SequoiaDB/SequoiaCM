package com.sequoiacm.schedule.dao.sequoiadb;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

public class SdbBSONObjectCursor implements ScmBSONObjectCursor {
    private static final Logger logger = LoggerFactory.getLogger(SdbBSONObjectCursor.class);

    SdbDataSourceWrapper datasource;
    Sequoiadb sdb;
    DBCursor cursor;

    public SdbBSONObjectCursor(SdbDataSourceWrapper datasource, Sequoiadb sdb, DBCursor cursor) {
        this.datasource = datasource;
        this.sdb = sdb;
        this.cursor = cursor;
    }

    @Override
    public BSONObject next() throws Exception {
        return cursor.getNext();
    }

    @Override
    public void close() {
        SdbDaoCommon.closeCursor(cursor);
        cursor = null;

        if (null != datasource) {
            datasource.releaseConnection(sdb);
        }

        datasource = null;
    }

    @Override
    public boolean hasNext() throws Exception {
        return cursor.hasNext();
    }

}
