package com.sequoiacm.mq.server.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.common.TableMetaCommon;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

@Component
public class SdbTemplate {
    private static final Logger logger = LoggerFactory.getLogger(SdbTemplate.class);

    private final SequoiadbDatasource datasource;

    @Autowired
    public SdbTemplate(SequoiadbDatasource datasource) {
        this.datasource = datasource;
    }

    public SequoiadbCollectionTemplate collection(String collectionSpace, String collection) {
        return new SequoiadbCollectionTemplate(collectionSpace, collection);
    }

    public SequoiadbCollectionTemplate collection(String clFullName) {
        String[] csCl = parseClFullName(clFullName);
        return new SequoiadbCollectionTemplate(csCl[0], csCl[1]);
    }

    public void dropCollection(String clFullName, boolean skipRecycleBin) {
        String[] csCl = parseClFullName(clFullName);
        Sequoiadb sdb = getSequoiadb();
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csCl[0]);
            TableMetaCommon.dropCLWithSkipRecycleBin(cs, csCl[1], skipRecycleBin);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                    || e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                return;
            }
            throw e;
        }
        finally {
            releaseSequoiadb(sdb);
        }
    }

    private String[] parseClFullName(String clFullName) {
        String[] csCl = clFullName.split("\\.");
        if (csCl.length != 2) {
            throw new BaseException(SDBError.SDB_INVALIDARG,
                    "invalid collection full name:" + clFullName);
        }
        return csCl;
    }

    public void createCollection(String clFullName, BSONObject option) {
        String[] csCl = parseClFullName(clFullName);
        createCollection(csCl[0], csCl[1], option);
    }

    public void createCollection(String csName, String clName, BSONObject option) {
        Sequoiadb sdb = getSequoiadb();
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            cs.createCollection(clName, option);
        }
        finally {
            releaseSequoiadb(sdb);
        }
    }

    public void createIndex(String csName, String clName, String idxName, BSONObject key,
            boolean isUnique, boolean enforced) {
        Sequoiadb sdb = getSequoiadb();
        try {
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cl.createIndex(idxName, key, isUnique, enforced);
        }
        finally {
            releaseSequoiadb(sdb);
        }
    }

    Sequoiadb getSequoiadb() {
        try {
            Sequoiadb db = datasource.getConnection();
            if (logger.isDebugEnabled()) {
                logger.debug("acquired connection from pool to sequoiadb, nodeName: {}.",
                        db.getNodeName());
            }
            return db;
        }
        catch (InterruptedException e) {
            throw new BaseException(SDBError.SDB_INTERRUPT,
                    "failed to get connection from sdb datasource", e);
        }
    }

    void releaseSequoiadb(Sequoiadb sdb) {
        if (sdb != null) {
            datasource.releaseConnection(sdb);
        }
    }

    public class SequoiadbCollectionTemplate {
        private final String collectionSpace;
        private final String collection;

        public SequoiadbCollectionTemplate(String collectionSpace, String collection) {
            this.collectionSpace = collectionSpace;
            this.collection = collection;
        }

        private Sequoiadb getSequoiadb(SdbTransaction context) {
            if (null == context) {
                try {
                    return datasource.getConnection();
                }
                catch (InterruptedException e) {
                    throw new BaseException(SDBError.SDB_INTERRUPT, e);
                }
            }
            else {
                return context.getSequoiadb();
            }
        }

        private void releaseSequoiadb(Sequoiadb sdb, SdbTransaction context) {
            if (null == context) {
                SdbTemplate.this.releaseSequoiadb(sdb);
            }
            else {
                // do nothing
            }
        }

        public void insert(BSONObject obj, SdbTransaction context) {
            Sequoiadb sdb;
            sdb = getSequoiadb(context);

            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).insert(obj);
            }
            finally {
                releaseSequoiadb(sdb, context);
            }
        }

        public long count(BSONObject matcher) {
            Sequoiadb sdb = getSequoiadb(null);
            try {
                return sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .getCount(matcher);
            }
            finally {
                releaseSequoiadb(sdb, null);
            }
        }

        public void insert(BSONObject obj) {
            insert(obj, null);
        }

        public void update(BSONObject matcher, BSONObject modifier, SdbTransaction context) {
            Sequoiadb sdb = getSequoiadb(context);
            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).update(matcher,
                        modifier, null);
            }
            finally {
                releaseSequoiadb(sdb, context);
            }
        }

        public void update(BSONObject matcher, BSONObject modifier) {
            update(matcher, modifier, null);
        }

        public void delete(BSONObject matcher, SdbTransaction context) {
            Sequoiadb sdb = getSequoiadb(context);
            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).delete(matcher);
            }
            finally {
                releaseSequoiadb(sdb, context);
            }
        }

        public void delete(BSONObject matcher) {
            delete(matcher, null);
        }

        public BSONObject findOne(BSONObject matcher, BSONObject orderby) {
            Sequoiadb sdb = getSequoiadb(null);
            try {
                return sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .queryOne(matcher, null, orderby, null, 0);
            }
            finally {
                datasource.releaseConnection(sdb);
            }
        }

        public BSONObject findOne(BSONObject matcher) {
            return findOne(matcher, null);
        }

        public List<BSONObject> find(BSONObject matcher) {
            return find(matcher, null, 0, -1);
        }

        public BSONObject findOneAndModify(BSONObject matcher, BSONObject modifier,
                boolean returnNew) {
            Sequoiadb sdb = getSequoiadb(null);
            DBCursor cursor = null;
            try {
                cursor = sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .queryAndUpdate(matcher, null, null, null, modifier, 0, 1, 0, returnNew);
                if (cursor != null && cursor.hasNext()) {
                    return cursor.getNext();
                }
                return null;
            }
            finally {
                closeCursor(cursor);
                datasource.releaseConnection(sdb);
            }
        }

        public List<BSONObject> find(BSONObject matcher, BSONObject orderBy, long skip,
                long limit) {
            List<BSONObject> objs = new ArrayList<>();
            Sequoiadb sdb = getSequoiadb(null);
            DBCursor cursor = null;
            try {
                cursor = sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .query(matcher, null, orderBy, null, skip, limit);

                if (cursor != null) {
                    while (cursor.hasNext()) {
                        objs.add(cursor.getNext());
                    }
                }
            }
            finally {
                closeCursor(cursor);
                datasource.releaseConnection(sdb);
            }
            return objs;
        }

        private void closeCursor(DBCursor cursor) {
            if (null != cursor) {
                try {
                    cursor.close();
                }
                catch (Exception e) {
                    logger.warn("close cursor failed", e);
                }
            }
        }

        public void ensureIndex(String indexName, BSONObject indexDefinition, boolean isUnique) {
            Sequoiadb sdb;
            try {
                sdb = datasource.getConnection();
            }
            catch (InterruptedException e) {
                throw new BaseException(SDBError.SDB_INTERRUPT, e);
            }

            try {
                DBCollection cl = sdb.getCollectionSpace(collectionSpace).getCollection(collection);
                DBCursor cursor = cl.getIndex(indexName);
                if (cursor != null) {
                    if (cursor.hasNext()) {
                        cursor.close();
                        return;
                    }
                    cursor.close();
                }

                try {
                    cl.createIndex(indexName, indexDefinition, isUnique, false);
                }
                catch (BaseException e) {
                    if (e.getErrorCode() != SDBError.SDB_IXM_EXIST.getErrorCode()
                            && e.getErrorCode() != SDBError.SDB_IXM_REDEF.getErrorCode()
                            && e.getErrorCode() != SDBError.SDB_IXM_EXIST_COVERD_ONE
                                    .getErrorCode()) {
                        throw e;
                    }
                }
            }
            finally {
                datasource.releaseConnection(sdb);
            }
        }
    }
}
