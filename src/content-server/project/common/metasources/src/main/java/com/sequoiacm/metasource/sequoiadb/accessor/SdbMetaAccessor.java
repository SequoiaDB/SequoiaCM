package com.sequoiacm.metasource.sequoiadb.accessor;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SdbMetaCursor;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbTransactionContext;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.DBQuery;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SdbMetaAccessor implements MetaAccessor {
    private SdbMetaSource metasource;
    private String csName;
    private String clName;
    private SdbTransactionContext context;
    private Logger logger = LoggerFactory.getLogger(SdbMetaAccessor.class);

    public SdbMetaAccessor(SdbMetaSource metasource, String csName, String clName,
            TransactionContext context) {
        this.metasource = metasource;
        this.csName = csName;
        this.clName = clName;
        this.context = (SdbTransactionContext) context;
    }

    public SdbMetaAccessor(SdbMetaSource metasource, String csName, String clName) {
        this(metasource, csName, clName, null);
    }

    Sequoiadb getConnection() throws SdbMetasourceException {
        if (context != null) {
            return context.getConnection();
        }
        return metasource.getConnection();
    }

    void releaseConnection(Sequoiadb sdb) {
        if (context != null) {
            // the context close will release this connection
            return;
        }
        if (null != sdb && null != metasource) {
            metasource.releaseConnection(sdb);
        }
    }

    SdbMetaSource getMetaSource() {
        return this.metasource;
    }

    String getCsName() {
        return csName;
    }

    String getClName() {
        return clName;
    }

    @Override
    public void insert(BSONObject insertor) throws ScmMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            cl.insert(insertor);
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(),
                    "insert failed:table=" + csName + "." + clName + ",inserto=" + insertor, e);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "insert failed:table=" + csName + "." + clName + ",inserto=" + insertor, e);
        }
        finally {
            releaseConnection(sdb);
        }
    }

    boolean deleteAndCheck(BSONObject deletor) throws SdbMetasourceException {
        BSONObject res = queryAndDelete(deletor);
        if (res == null) {
            return false;
        }
        else {
            return true;
        }
    }

    // return an matching record (old), and delete all matching records.
    public BSONObject queryAndDelete(BSONObject deletor) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            cursor = cl.queryAndRemove(deletor, null, null, null, 0, -1,
                    DBQuery.FLG_QUERY_WITH_RETURNDATA);
            BSONObject ret = null;
            while (cursor.hasNext()) {
                ret = cursor.getNext();
            }
            return ret;
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(),
                    "delete failed:table=" + csName + "." + clName + ",deletor=" + deletor, e);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + csName + "." + clName + ",deletor=" + deletor, e);
        }
        finally {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
        }
    }

    public void delete(BSONObject deletor) throws SdbMetasourceException {
        delete(deletor, null);
    }

    void delete(BSONObject deletor, BSONObject hint) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            cl.delete(deletor, hint);
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(),
                    "delete failed:table=" + csName + "." + clName + ",deletor=" + deletor, e);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete failed:table=" + csName + "." + clName + ",deletor=" + deletor, e);
        }
        finally {
            releaseConnection(sdb);
        }
    }

    @Override
    public void update(BSONObject matcher, BSONObject updater) throws SdbMetasourceException {
        update(matcher, updater, null);
    }

    void update(BSONObject matcher, BSONObject updater, BSONObject hint)
            throws SdbMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            cl.update(matcher, updater, hint);
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "update table failed:table=" + csName
                    + "." + clName + ",matcher=" + matcher + ",updater=" + updater, e);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update table failed:table=" + csName + "." + clName + ",matcher=" + matcher
                            + ",updater=" + updater,
                    e);
        }
        finally {
            releaseConnection(sdb);
        }
    }

    boolean updateAndCheck(BSONObject matcher, BSONObject updator) throws SdbMetasourceException {
        BSONObject res = queryAndUpdate(matcher, updator, null);
        if (res == null) {
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws SdbMetasourceException {
        return query(matcher, selector, orderBy, 0, -1);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip,
            long limit) throws SdbMetasourceException {
        return query(matcher, selector, orderBy, skip, limit, 0);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip,
            long limit, int flag) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        SdbMetaCursor sdbCursor = null;
        DBCursor cursor = null;

        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }
            cursor = cl.query(matcher, selector, orderBy, null, skip, limit, flag);
            sdbCursor = new SdbMetaCursor(getMetaSource(), sdb, cursor);
        }
        catch (SdbMetasourceException e) {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
            throw e;
        }
        catch (BaseException e) {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
            throw new SdbMetasourceException(e.getErrorCode(), "query failed:csName=" + getCsName()
                    + ",clName=" + getClName() + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(), "query failed:csName="
                    + getCsName() + ",clName=" + getClName() + ",matcher=" + matcher, e);
        }
        return sdbCursor;
    }

    @Override
    public long count(BSONObject matcher) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            return cl.getCount(matcher);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "count failed:csName=" + getCsName()
                    + ",clName=" + getClName() + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(), "count failed:csName="
                    + getCsName() + ",clName=" + getClName() + ",matcher=" + matcher, e);
        }
        finally {
            releaseConnection(sdb);
        }
    }

    @Override
    public double sum(BSONObject matcher, String field) throws ScmMetasourceException {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        List<BSONObject> objs = new ArrayList<>();
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            BSONObject match = new BasicBSONObject("$match", matcher);
            BSONObject groupVal = new BasicBSONObject();
            BSONObject group = new BasicBSONObject("$group", groupVal);
            groupVal.put("_id", null);
            groupVal.put("total", new BasicBSONObject("$sum", "$" + field));
            objs.add(match);
            objs.add(group);
            cursor = cl.aggregate(objs);
            if (cursor.hasNext()) {
                Object obj = cursor.getNext().get("total");
                if (obj != null) {
                    return (double) obj;
                }
            }
            return 0;
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(),
                    "sum failed:csName=" + getCsName() + ",clName=" + getClName() + ",objs=" + objs,
                    e);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "sum failed:csName=" + getCsName() + ",clName=" + getClName() + ",objs=" + objs,
                    e);
        }
        finally {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
        }
    }

    // return an matching record (old), and update all matching records.
    public BSONObject queryAndUpdate(BSONObject matcher, BSONObject updator, BSONObject hint)
            throws SdbMetasourceException {
        return queryAndUpdate(matcher, updator, hint, false);
    }

    // return an matching record (old|new), and update all matching records.
    public BSONObject queryAndUpdate(BSONObject matcher, BSONObject updator, BSONObject hint,
            boolean returnNew) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        DBCursor cursor = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }
            cursor = cl.queryAndUpdate(matcher, null, null, hint, updator, 0, -1,
                    DBQuery.FLG_QUERY_WITH_RETURNDATA, returnNew);
            BSONObject ret = null;
            while (cursor.hasNext()) {
                ret = cursor.getNext();
            }
            return ret;
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "update table failed:table=" + csName
                    + "." + clName + ",matcher=" + matcher + ",updator=" + updator, e);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update table failed:table=" + csName + "." + clName + ",matcher=" + matcher
                            + ",updator=" + updator,
                    e);
        }
        finally {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
        }
    }

    @Override
    public void ensureTable(List<String> indexFields, List<String> uniqueIndexField)
            throws ScmMetasourceException {
        ensureCollection();
        if (indexFields != null) {
            for (String idxField : indexFields) {
                ensureIndex("idx_" + idxField, new BasicBSONObject(idxField, 1), false);
            }
        }
        if (uniqueIndexField != null) {
            for (String idxField : uniqueIndexField) {
                ensureIndex("idx_" + idxField, new BasicBSONObject(idxField, 1), true);
            }
        }
    }

    @Override
    public void upsert(BSONObject matcher, BSONObject updator) throws ScmMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }
            cl.upsert(matcher, updator, null);
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "upsert failed:table=" + csName + "."
                    + clName + ",matcher=" + matcher + ", updater=" + updator, e);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(), "upsert failed:table="
                    + csName + "." + clName + ",matcher=" + matcher + ", updater=" + updator, e);
        }
        finally {
            releaseConnection(sdb);
        }
    }

    private void ensureIndex(String idxName, BSONObject indexDefinition, boolean isUnique)
            throws SdbMetasourceException {
        Sequoiadb db = getConnection();
        try {
            CollectionSpace cs = db.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            DBCursor cursor = cl.getIndex(idxName);
            if (cursor != null) {
                try {
                    if (cursor.hasNext()) {
                        cursor.close();
                        return;
                    }
                }
                finally {
                    cursor.close();
                }
            }
            cl.createIndex(idxName, indexDefinition, isUnique, false);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_EXIST.getErrorCode()
                    && e.getErrorCode() != SDBError.SDB_IXM_REDEF.getErrorCode()
                    && e.getErrorCode() != SDBError.SDB_IXM_EXIST_COVERD_ONE.getErrorCode()) {
                throw new SdbMetasourceException(e.getErrorCode(),
                        "failed to create cl index:" + csName + "." + clName + ", idxName="
                                + idxName + ", idxDef=" + indexDefinition + ", isUnique="
                                + isUnique,
                        e);
            }
        }
        finally {
            releaseConnection(db);
        }
    }

    private void ensureCollection() throws SdbMetasourceException {
        Sequoiadb db = getConnection();
        try {
            CollectionSpace cs = db.getCollectionSpace(csName);
            if (cs.isCollectionExist(clName)) {
                return;
            }
            cs.createCollection(clName);
            logger.info("create collection:{}.{}, option={}", csName, clName);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode()) {
                return;
            }
            throw new SdbMetasourceException(e.getErrorCode(),
                    "failed to create cl:" + csName + "." + clName, e);
        }
        finally {
            releaseConnection(db);
        }
    }

    boolean isInTransaction() {
        if (context != null) {
            return context.isBegin();
        }
        return false;
    }

    @Override
    public BSONObject queryOne(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            return cl.queryOne(matcher, selector, orderBy, null, DBQuery.FLG_QUERY_WITH_RETURNDATA);
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "query failed:csName=" + getCsName()
                    + ",clName=" + getClName() + ",matcher=" + matcher, e);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(), "query failed:csName="
                    + getCsName() + ",clName=" + getClName() + ",matcher=" + matcher, e);
        }
        finally {
            releaseConnection(sdb);
        }
    }

    protected MetaCursor aggregate(List<BSONObject> objs) throws SdbMetasourceException {
        Sequoiadb sdb = null;
        SdbMetaCursor sdbCursor = null;
        DBCursor cursor = null;

        try {
            sdb = getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(getCsName());
            DBCollection cl = cs.getCollection(getClName());
            if (null == cl) {
                throw new SdbMetasourceException(SDBError.SDB_DMS_NOTEXIST.getErrorCode(),
                        "getCollection failed:cl=" + getCsName() + "." + getClName());
            }

            cursor = cl.aggregate(objs);
            sdbCursor = new SdbMetaCursor(getMetaSource(), sdb, cursor);
        }
        catch (SdbMetasourceException e) {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
            throw e;
        }
        catch (BaseException e) {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
            throw new SdbMetasourceException(e.getErrorCode(), "aggregate failed:csName="
                    + getCsName() + ",clName=" + getClName() + ",objs=" + objs, e);
        }
        catch (Exception e) {
            SequoiadbHelper.closeCursor(cursor);
            releaseConnection(sdb);
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "aggregate failed:csName=" + getCsName() + ",clName=" + getClName() + ",objs="
                            + objs,
                    e);
        }
        return sdbCursor;
    }
}
