package com.sequoiacm.config.metasource.sequoiadb;

import com.sequoiadb.base.*;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.TableDaoBase;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

import java.util.List;

public class SequoiadbTableDao extends TableDaoBase {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbTableDao.class);
    private SequoiadbMetasource sdbMetasource;
    private String csName;
    private String clName;
    private SequoiadbTransaction transaction;

    private static final int SDB_IXM_CREATING = -387; // DB error code
    private static final int SDB_IXM_COVER_CREATING = -389; // DB error code

    public SequoiadbTableDao(SequoiadbMetasource sdbMetasource, String csName, String clName) {
        this.sdbMetasource = sdbMetasource;
        this.csName = csName;
        this.clName = clName;
    }

    public SequoiadbTableDao(Transaction transaction, String csName, String clName) {
        this.transaction = (SequoiadbTransaction) transaction;
        this.csName = csName;
        this.clName = clName;
    }

    @Override
    public void delete(BSONObject matcher) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            cl.delete(matcher);
        }
        catch (Exception e) {
            throw new MetasourceException(
                    "delete failed:csName=" + csName + ",clName=" + clName + ",matcher=" + matcher,
                    e);
        }
        finally {
            releaseConnection(db);
        }
    }

    @Override
    public void insert(BSONObject record) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            cl.insert(record);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new MetasourceException(ScmConfError.METASOURCE_RECORD_EXIST,
                        "record alredy exist:csName=" + csName + ",clName=" + clName + ",record="
                                + record,
                        e);
            }
            throw new MetasourceException(
                    "insert failed:csName=" + csName + ",clName=" + clName + ",record=" + record,
                    e);
        }
        catch (Exception e) {
            throw new MetasourceException(
                    "insert failed:csName=" + csName + ",clName=" + clName + ",record=" + record,
                    e);
        }
        finally {
            releaseConnection(db);
        }
    }

    void _update(BSONObject matcher, BSONObject updator) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            cl.update(matcher, updator, null);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new MetasourceException(ScmConfError.METASOURCE_RECORD_EXIST,
                        "record alredy exist:csName=" + csName + ",clName=" + clName + ",matcher="
                                + matcher + ",updator=" + updator,
                        e);
            }
            throw new MetasourceException("update failed:csName=" + csName + ",clName=" + clName
                    + ",matcher=" + matcher + ",updator=" + updator, e);
        }
        catch (Exception e) {
            throw new MetasourceException("update failed:csName=" + csName + ",clName=" + clName
                    + ",matcher=" + matcher + ",updator=" + updator, e);
        }
        finally {
            releaseConnection(db);
        }
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws MetasourceException {
        return query(matcher, selector, orderBy, 0, -1);
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip,
            long limit) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            DBCursor c = cl.query(matcher, selector, orderBy, null, skip, limit);
            return new SequoiadbMetaCursor(sdbMetasource, db, c);
        }
        catch (BaseException e) {
            releaseConnection(db);
            if (e.getErrorCode() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                    || e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                throw new MetasourceException(ScmConfError.METASOURCE_TABLE_NOT_EXIST,
                        "get collection failed: csName=" + csName + ",clName=" + clName, e);
            }
            throw new MetasourceException(
                    "query failed:csName=" + csName + ",clName=" + clName + ",matcher=" + matcher,
                    e);
        }
        catch (Exception e) {
            releaseConnection(db);
            throw new MetasourceException(
                    "query failed:csName=" + csName + ",clName=" + clName + ",matcher=" + matcher,
                    e);
        }
    }

    private Sequoiadb getConnection() throws MetasourceException {
        if (transaction != null) {
            return transaction.getConnection();
        }
        return sdbMetasource.getConnection();
    }

    private void releaseConnection(Sequoiadb db) {
        if (db == null) {
            return;
        }
        if (transaction != null) {
            return;
        }
        sdbMetasource.releaseConnection(db);

    }

    @Override
    public BSONObject deleteAndCheck(BSONObject matcher) throws MetasourceException {
        DBCursor cursor = null;
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            cursor = cl.queryAndRemove(matcher, null, null, null, 0, -1,
                    DBQuery.FLG_QUERY_WITH_RETURNDATA);
            if (!cursor.hasNext()) {
                return null;
            }

            BSONObject ret = null;
            while (cursor.hasNext()) {
                ret = cursor.getNext();
            }
            return ret;
        }
        catch (Exception e) {
            throw new MetasourceException(
                    "delete failed:csName=" + csName + ",clName=" + clName + ",matcher=" + matcher,
                    e);
        }
        finally {
            closeCursor(cursor);
            releaseConnection(db);
        }
    }

    @Override
    public BSONObject updateAndReturnNew(BSONObject matcher, BSONObject updator)
            throws MetasourceException {
        DBCursor cursor = null;
        Sequoiadb db = getConnection();
        try {

            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            cursor = cl.queryAndUpdate(matcher, null, null, null, updator, 0, -1,
                    DBQuery.FLG_QUERY_WITH_RETURNDATA, true);
            BSONObject ret = null;
            while (cursor.hasNext()) {
                ret = cursor.getNext();
            }
            return ret;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new MetasourceException(ScmConfError.METASOURCE_RECORD_EXIST,
                        "record alredy exist:csName=" + csName + ",clName=" + clName + ",matcher="
                                + matcher + ",updator=" + updator,
                        e);
            }
            throw new MetasourceException("update failed:csName=" + csName + ",clName=" + clName
                    + ",matcher=" + matcher + ",updator=" + updator, e);
        }
        catch (Exception e) {
            throw new MetasourceException("update failed:csName=" + csName + ",clName=" + clName
                    + ",matcher=" + matcher + ",updator=" + updator, e);
        }
        finally {
            closeCursor(cursor);
            releaseConnection(db);
        }
    }

    private void closeCursor(DBCursor c) {
        try {
            if (c != null) {
                c.close();
            }
        }
        catch (Exception e) {
            logger.warn("failed to close cursor", e);
        }
    }

    @Override
    public void update(BSONObject matcher, BSONObject updator) throws MetasourceException {
        updator = new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, updator);
        _update(matcher, updator);
    }

    @Override
    public BSONObject updateAndCheck(BSONObject matcher, BSONObject updator)
            throws MetasourceException {
        updator = new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, updator);
        return updateAndReturnNew(matcher, updator);
    }

    @Override
    public long count(BSONObject matcher) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            return cl.getCount(matcher);
        }
        catch (Exception e) {
            throw new MetasourceException(
                    "count failed:csName=" + csName + ",clName=" + clName + ",matcher=" + matcher,
                    e);
        }
        finally {
            releaseConnection(db);
        }
    }

    @Override
    public void ensureTable(List<String> indexFields, List<String> uniqueIndexField)
            throws MetasourceException {
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
    public void ensureTable(List<IndexDef> indexes) throws MetasourceException {
        ensureCollection();
        if (indexes != null) {
            for (IndexDef idxDef : indexes) {
                StringBuilder idxName = new StringBuilder();
                idxName.append("idx");
                BSONObject indexKeys = new BasicBSONObject();
                for (String idxField : idxDef.getUnionKeys()) {
                    indexKeys.put(idxField, 1);
                    idxName.append("_").append(idxField);
                }

                ensureIndex(idxName.toString(), indexKeys, idxDef.isUnique());
            }
        }
    }

    protected void ensureCollection() throws MetasourceException {
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
            throw new MetasourceException("failed to create cl:" + csName + "." + clName, e);
        }
        finally {
            releaseConnection(db);
        }
    }

    protected boolean isDomainExist(String domainName) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            return db.isDomainExist(domainName);
        }
        finally {
            releaseConnection(db);
        }
    }

    protected void ensureIndex(String idxName, BSONObject indexDefinition, boolean isUnique)
            throws MetasourceException {
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
                    && e.getErrorCode() != SDBError.SDB_IXM_EXIST_COVERD_ONE.getErrorCode()
                    && e.getErrorCode() != SDB_IXM_CREATING
                    && e.getErrorCode() != SDB_IXM_COVER_CREATING) {
                throw new MetasourceException("failed to create cl index:" + csName + "." + clName
                        + ", idxName=" + idxName + ", idxDef=" + indexDefinition + ", isUnique="
                        + isUnique, e);
            }
        }
        finally {
            releaseConnection(db);
        }
    }

    public void upsert(BSONObject matcher, BSONObject updator) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(csName, clName, db);
            cl.upsert(matcher, updator, null);
        }
        catch (Exception e) {
            throw new MetasourceException("upsert failed:csName=" + csName + ",clName=" + clName
                    + ",matcher=" + matcher + ",updator=" + updator, e);
        }
        finally {
            releaseConnection(db);
        }
    }
}
