package com.sequoiacm.metasource.sequoiadb;

import org.bson.BSONObject;

import com.sequoiacm.metasource.MetaCursor;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SdbMetaCursor implements MetaCursor {
    private SdbMetaSource metasource;
    private Sequoiadb sdb;
    private DBCursor cursor;

    // 托管 sdb 连接，游标释放的时候，将连接进行回池
    public SdbMetaCursor(SdbMetaSource metasource, Sequoiadb sdb, DBCursor cursor) {
        this.metasource = metasource;
        this.sdb = sdb;
        this.cursor = cursor;
    }

    // sdb 连接在外部维护，游标释放只关闭底层sdb cursor，调用者需要自行维护该游标关联的 sdb 连接
    public SdbMetaCursor(DBCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public boolean hasNext() throws SdbMetasourceException {
        if (null == cursor) {
            return false;
        }

        try {
            return cursor.hasNext();
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "failed to hasNext", e);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(), "failed to hasNext",
                    e);
        }
    }

    @Override
    public BSONObject getNext() throws SdbMetasourceException {
        if (null == cursor) {
            return null;
        }

        try {
            BSONObject o = cursor.getNext();
            if (null != o) {
                o.removeField("_id");
            }

            return o;
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "failed to hasNext", e);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(), "failed to hasNext",
                    e);
        }
    }

    @Override
    public void close() {
        SequoiadbHelper.closeCursor(cursor);

        if (null != sdb && null != metasource) {
            metasource.releaseConnection(sdb);
        }

        cursor = null;
        sdb = null;
        metasource = null;
    }

}
