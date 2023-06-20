package com.sequoiacm.cloud.authentication.dao;

import java.util.ArrayList;
import java.util.List;

import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SequoiadbTemplate {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbTemplate.class);

    private final SequoiadbDatasource datasource;

    public SequoiadbTemplate(SequoiadbDatasource datasource) {
        this.datasource = datasource;
    }

    public SequoiadbCollectionTemplate collection(String collectionSpace, String collection) {
        return new SequoiadbCollectionTemplate(collectionSpace, collection);
    }

    public class SequoiadbCollectionTemplate {
        private final String collectionSpace;
        private final String collection;

        public SequoiadbCollectionTemplate(String collectionSpace, String collection) {
            this.collectionSpace = collectionSpace;
            this.collection = collection;
        }

        private Sequoiadb getSequoiadb(SequoiadbTransaction context) {
            if (null == context) {
                try {
                    Sequoiadb db = datasource.getConnection();
                    if (logger.isDebugEnabled()) {
                        logger.debug("acquired connection from pool to sequoiadb, nodeName: {}.",
                                db.getNodeName());
                    }
                    return db;
                }
                catch (InterruptedException e) {
                    throw new BaseException(SDBError.SDB_INTERRUPT, e);
                }
            }
            else {
                return context.getSequoiadb();
            }
        }

        private void releaseSequoiadb(Sequoiadb sdb, SequoiadbTransaction context) {
            if (null == context) {
                datasource.releaseConnection(sdb);
            }
            else {
                // do nothing
            }
        }

        public void insert(BSONObject obj, SequoiadbTransaction context) {
            Sequoiadb sdb;
            sdb = getSequoiadb(context);

            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).insert(obj);
            }
            finally {
                releaseSequoiadb(sdb, context);
            }
        }

        public void insert(BSONObject obj) {
            insert(obj, null);
        }

        public void update(BSONObject matcher, BSONObject modifier, SequoiadbTransaction context) {
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

        public void delete(BSONObject matcher, SequoiadbTransaction context) {
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

        public BSONObject findOne(BSONObject matcher) {
            Sequoiadb sdb;
            try {
                sdb = datasource.getConnection();
            }
            catch (InterruptedException e) {
                throw new BaseException(SDBError.SDB_INTERRUPT, e);
            }

            try {
                return sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .queryOne(matcher, null, null, null, 0);
            }
            finally {
                datasource.releaseConnection(sdb);
            }
        }

        public List<BSONObject> find(BSONObject matcher) {
            return find(matcher, null, 0, -1);
        }

        public List<BSONObject> find(BSONObject matcher, BSONObject orderBy, long skip,
                long limit) {
            List<BSONObject> objs = new ArrayList<>();
            Sequoiadb sdb;
            try {
                sdb = datasource.getConnection();
            }
            catch (InterruptedException e) {
                throw new BaseException(SDBError.SDB_INTERRUPT, e);
            }

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

        public long count(BSONObject matcher) {
            Sequoiadb sdb;
            try {
                sdb = datasource.getConnection();
            }
            catch (InterruptedException e) {
                throw new BaseException(SDBError.SDB_INTERRUPT, e);
            }

            try {
                return sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .getCount(matcher);
            }
            finally {
                datasource.releaseConnection(sdb);
            }
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
