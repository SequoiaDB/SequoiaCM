package com.sequoiacm.schedule.dao;

import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.dao.sequoiadb.SdbDataSourceWrapper;
import com.sequoiacm.schedule.dao.sequoiadb.TransactionImpl;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequoiadbTemplate {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbTemplate.class);

    private final SdbDataSourceWrapper datasource;

    public SequoiadbTemplate(SdbDataSourceWrapper datasource) {
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

        private Sequoiadb getSequoiadb(TransactionImpl context) throws Exception {
            if (null == context) {
                Sequoiadb db = datasource.getConnection();
                if (logger.isDebugEnabled()) {
                    logger.debug("acquired connection from pool to sequoiadb, nodeName: {}.",
                            db.getNodeName());
                }
                return db;
            }
            else {
                return context.getSequoiadb();
            }
        }

        private void releaseSequoiadb(Sequoiadb sdb, TransactionImpl context) {
            if (null == context) {
                datasource.releaseConnection(sdb);
            }
        }

        public void insert(BSONObject obj, TransactionImpl context) throws Exception {
            Sequoiadb sdb;
            sdb = getSequoiadb(context);

            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).insert(obj);
            }
            finally {
                releaseSequoiadb(sdb, context);
            }
        }

        public void insert(BSONObject obj) throws Exception {
            insert(obj, null);
        }

        public void update(BSONObject matcher, BSONObject modifier, TransactionImpl context)
                throws Exception {
            Sequoiadb sdb = getSequoiadb(context);
            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).update(matcher,
                        modifier, null);
            }
            finally {
                releaseSequoiadb(sdb, context);
            }
        }

        public void update(BSONObject matcher, BSONObject modifier) throws Exception {
            update(matcher, modifier, null);
        }

        public void delete(BSONObject matcher, TransactionImpl context) throws Exception {
            Sequoiadb sdb = getSequoiadb(context);
            try {
                sdb.getCollectionSpace(collectionSpace).getCollection(collection).delete(matcher);
            }
            finally {
                releaseSequoiadb(sdb, context);
            }
        }

        public void delete(BSONObject matcher) throws Exception {
            delete(matcher, null);
        }

        public void ensureTable() throws Exception {
            BasicBSONObject clOption = new BasicBSONObject();
            Sequoiadb db = null;
            try {
                db = getSequoiadb(null);
                CollectionSpace cs = db.getCollectionSpace(collectionSpace);
                if (cs.isCollectionExist(collection)) {
                    return;
                }
                cs.createCollection(collection, clOption);
                logger.info("create collection:{}.{}, option={}", collectionSpace, collection,
                        clOption);
            }
            catch (BaseException e) {
                if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode()) {
                    return;
                }
                throw new ScheduleException(e.getErrorType(), "failed to create cl:"
                        + collectionSpace + "." + collection + ", option=" + clOption, e);
            }
            finally {
                releaseSequoiadb(db, null);
            }
        }

        public void ensureIndex(String indexName, BSONObject key, boolean isUnique,
                boolean enforced) throws ScheduleException {
            Sequoiadb db = null;
            try {
                db = getSequoiadb(null);
                CollectionSpace cs = db.getCollectionSpace(collectionSpace);
                DBCollection cl = cs.getCollection(collection);
                try {
                    cl.getIndexInfo(indexName);
                    return;
                }
                catch (BaseException e) {
                    if (e.getErrorCode() != SDBError.SDB_IXM_NOTEXIST.getErrorCode()) {
                        throw e;
                    }
                }

                try {
                    cl.createIndex(indexName, key, isUnique, enforced);
                }
                catch (Exception e) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                            "create index failed:csName=" + collectionSpace + ",clName="
                                    + collection + ",indexName=" + indexName + ",key=" + key,
                            e);
                }
            }
            catch (Exception e) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "ensure index failed:csName=" + collectionSpace + ",clName=" + collection
                                + ",indexName=" + indexName + ",key=" + key,
                        e);
            }
            finally {
                releaseSequoiadb(db, null);
            }
        }
    }
}
