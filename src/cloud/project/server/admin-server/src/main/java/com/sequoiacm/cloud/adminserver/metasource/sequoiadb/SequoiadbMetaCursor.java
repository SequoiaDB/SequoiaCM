package com.sequoiacm.cloud.adminserver.metasource.sequoiadb;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

public class SequoiadbMetaCursor implements MetaCursor {
    private Logger logger = LoggerFactory.getLogger(SequoiadbMetaCursor.class);
    private SequoiadbMetaSource metasource;
    private Sequoiadb db;
    private DBCursor cursor;

    public SequoiadbMetaCursor(SequoiadbMetaSource metasource, Sequoiadb db, DBCursor cursor) {
        this.metasource = metasource;
        this.db = db;
        this.cursor = cursor;
    }

    // this cursor close will not release db (for transaction)
    public SequoiadbMetaCursor(Sequoiadb db, DBCursor cursor) {
        this.db = db;
        this.cursor = cursor;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return cursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        return cursor.getNext();
    }

    @Override
    public void close() {
        try {
            cursor.close();
        }
        catch (Exception e) {
            logger.warn("close sdb cursor failed", e);
        }
        finally {
            if (metasource != null) {
                metasource.releaseConnection(db);
            }
        }

    }

}
