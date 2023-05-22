package com.sequoiacm.infrastructure.metasource.template;

import com.sequoiacm.infrastructure.common.TableMetaCommon;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiacm.infrastructure.metasource.SdbMetaCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.DBQuery;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SequoiadbTemplate {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbTemplate.class);

    private DataSourceWrapper datasourceWrapper = DataSourceWrapper.getInstance();

    public SequoiadbCollectionSpaceTemplate collectionSpace(String collectionSpace) {
        return new SequoiadbCollectionSpaceTemplate(collectionSpace);
    }

    public SequoiadbCollectionTemplate collection(String collectionSpace, String collection) {
        return new SequoiadbCollectionTemplate(collectionSpace, collection);
    }

    protected Sequoiadb getSequoiadb(SequoiadbTransaction context) {
        if (null == context) {
            return datasourceWrapper.getConnection();
        }
        else {
            return context.getSequoiadb();
        }
    }

    protected void releaseSequoiadb(Sequoiadb sdb, SequoiadbTransaction context) {
        if (null == context) {
            datasourceWrapper.releaseConnection(sdb);
        }
        else {
            // do nothing
        }
    }

    public class SequoiadbCollectionSpaceTemplate {
        private final String collectionSpace;

        public SequoiadbCollectionSpaceTemplate(String collectionSpace) {
            this.collectionSpace = collectionSpace;
        }

        public SequoiadbCollectionTemplate createCollection(String collectionName) {
            Sequoiadb sdb = null;
            try {
                sdb = getSequoiadb(null);
                CollectionSpace cs = sdb.getCollectionSpace(collectionSpace);
                cs.createCollection(collectionName);
                return collection(collectionSpace, collectionName);
            }
            finally {
                releaseSequoiadb(sdb, null);
            }
        }

        public void dropCollection(String collectionName) {
            dropCollection(collectionName, false);
        }

        public void dropCollection(String collectionName, boolean skipRecycleBin) {
            Sequoiadb sdb = null;
            try {
                sdb = getSequoiadb(null);
                CollectionSpace cs = sdb.getCollectionSpace(collectionSpace);
                TableMetaCommon.dropCLWithSkipRecycleBin(cs, collectionName, skipRecycleBin);
            }
            finally {
                releaseSequoiadb(sdb, null);
            }
        }
    }

    public class SequoiadbCollectionTemplate {
        private final String collectionSpace;
        private final String collection;

        public SequoiadbCollectionTemplate(String collectionSpace, String collection) {
            this.collectionSpace = collectionSpace;
            this.collection = collection;
        }

        public void insert(BSONObject obj, SequoiadbTransaction context) {
            Sequoiadb sdb = getSequoiadb(context);
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

        public void upsert(BSONObject matcher, BSONObject modifier) {
            Sequoiadb sdb = getSequoiadb(null);
            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).upsert(matcher,
                        modifier, null);
            }
            finally {
                releaseSequoiadb(sdb, null);
            }
        }

        // return an matching record, and update all matching records.
        public BSONObject queryAndUpdate(BSONObject matcher, BSONObject selector,
                BSONObject modifier, boolean returnNew, SequoiadbTransaction context) {
            Sequoiadb sdb = getSequoiadb(context);
            DBCursor cursor = null;
            try {
                cursor = sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .queryAndUpdate(matcher, selector, null, null, modifier, 0, -1,
                                DBQuery.FLG_QUERY_WITH_RETURNDATA, returnNew);
                BSONObject ret = null;
                while (cursor.hasNext()) {
                    ret = cursor.getNext();
                }
                return ret;
            }
            finally {
                closeCursor(cursor);
                releaseSequoiadb(sdb, context);
            }
        }

        public BSONObject queryAndUpdate(BSONObject matcher, BSONObject selector,
                BSONObject modifier, boolean returnNew) {
            return queryAndUpdate(matcher, selector, modifier, returnNew, null);
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

        // return an matching record (old), and delete all matching records.
        public BSONObject queryAndDelete(BSONObject matcher, BSONObject selector,
                SequoiadbTransaction context) {
            Sequoiadb sdb = getSequoiadb(context);
            DBCursor cursor = null;
            try {
                cursor = sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .queryAndRemove(matcher, selector, null, null, 0, -1,
                                DBQuery.FLG_QUERY_WITH_RETURNDATA);
                BSONObject ret = null;
                while (cursor.hasNext()) {
                    ret = cursor.getNext();
                }
                return ret;
            }
            finally {
                closeCursor(cursor);
                releaseSequoiadb(sdb, context);
            }
        }

        public BSONObject queryAndDelete(BSONObject matcher, BSONObject selector) {
            return queryAndDelete(matcher, selector, null);
        }

        public BSONObject findOne(BSONObject matcher) {
            return findOne(matcher, null);
        }

        public BSONObject findOne(BSONObject matcher, BSONObject selector) {
            Sequoiadb sdb = datasourceWrapper.getConnection();
            try {
                return sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .queryOne(matcher, selector, null, null, 0);
            }
            finally {
                datasourceWrapper.releaseConnection(sdb);
            }
        }

        public MetaCursor find(BSONObject matcher) throws Exception {
            return find(matcher, null, null, 0, -1);
        }

        public MetaCursor find(BSONObject matcher, BSONObject selector, BSONObject orderBy,
                long skip, long limit) throws Exception {
            Sequoiadb sdb = datasourceWrapper.getConnection();
            MetaCursor meatCursor = null;
            DBCursor cursor = null;
            try {
                cursor = sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .query(matcher, selector, orderBy, null, skip, limit);
                meatCursor = new SdbMetaCursor(datasourceWrapper.getSdbDataSource(), sdb, cursor);
            }
            catch (Exception e) {
                closeCursor(cursor);
                datasourceWrapper.releaseConnection(sdb);
                throw e;
            }
            return meatCursor;
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
            Sequoiadb sdb = datasourceWrapper.getConnection();
            DBCursor cursor = null;
            try {
                DBCollection cl = sdb.getCollectionSpace(collectionSpace).getCollection(collection);
                cursor = cl.getIndex(indexName);
                if (cursor != null) {
                    if (cursor.hasNext()) {
                        return;
                    }
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
                closeCursor(cursor);
                datasourceWrapper.releaseConnection(sdb);
            }
        }

        public long count(BSONObject filter) {
            Sequoiadb sdb = datasourceWrapper.getConnection();
            try {
                return sdb.getCollectionSpace(collectionSpace).getCollection(collection)
                        .getCount(filter);
            }
            finally {
                datasourceWrapper.releaseConnection(sdb);
            }
        }
    }
}
