package com.sequoiacm.infrastructure.metasource;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.exception.BaseException;

public class SdbMetaCursor implements MetaCursor {
    private static final Logger logger = LoggerFactory.getLogger(SdbMetaCursor.class);
    private SequoiadbDatasource datasource;
    private Sequoiadb sdb;
    private DBCursor cursor;

    public SdbMetaCursor(SequoiadbDatasource sdbDataSource, Sequoiadb sdb, DBCursor cursor) {
        this.datasource = sdbDataSource;
        this.sdb = sdb;
        this.cursor = cursor;
    }

    @Override
    public boolean hasNext() throws BaseException {
        if (null == cursor) {
            return false;
        }
        return cursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws BaseException {
        if (null == cursor) {
            return null;
        }

        BSONObject o = cursor.getNext();
        if (null != o) {
            o.removeField("_id");
        }
        return o;

    }

    @Override
    public void close() {
        try {
            if (null != cursor) {
                cursor.close();
            }
        }
        catch (Exception e) {
            logger.warn("close cursor failed", e);
        }

        if (null != sdb && null != datasource) {
            datasource.releaseConnection(sdb);
        }

        cursor = null;
        sdb = null;
        datasource = null;
    }
}
