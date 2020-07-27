package com.sequoiacm.cloud.adminserver.metasource.sequoiadb;

import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SequoiadbMetaAccessor implements MetaAccessor {
    private SequoiadbMetaSource metasource;
    private String csName;
    private String clName;

    public SequoiadbMetaAccessor(SequoiadbMetaSource metasource, String csName, String clName) {
        this.metasource = metasource;
        this.csName = csName;
        this.clName = clName;
    }

    Sequoiadb getConnection() throws ScmMetasourceException {
        return metasource.getConnection();
    }

    void releaseConnection(Sequoiadb sdb) {
        if (null != sdb && null != metasource) {
            metasource.releaseConnection(sdb);
        }
    }

    SequoiadbMetaSource getMetaSource() {
        return this.metasource;
    }

    String getCsName() {
        return csName;
    }

    String getClName() {
        return clName;
    }

    @Override
    public void insert(BSONObject record) throws ScmMetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(db, csName, clName);
            cl.insert(record);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new ScmMetasourceException(StatisticsError.RECORD_EXISTS,
                        "record alredy exist:csName=" + csName + ",clName=" + clName + ",record="
                                + record,
                        e);
            }
            throw new ScmMetasourceException(
                    "insert failed:csName=" + csName + ",clName=" + clName + ",record=" + record,
                    e);
        }
        catch (Exception e) {
            throw new ScmMetasourceException(
                    "insert failed:csName=" + csName + ",clName=" + clName + ",record=" + record,
                    e);
        }
        finally {
            releaseConnection(db);
        }
    }

    @Override
    public void upsert(BSONObject record, BSONObject matcher) throws ScmMetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(db, csName, clName);
            cl.upsert(matcher, record, null);
        }
        catch (Exception e) {
            throw new ScmMetasourceException(
                    "insert failed:csName=" + csName + ",clName=" + clName + ",record=" + record,
                    e);
        }
        finally {
            releaseConnection(db);
        }
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(db, csName, clName);
            DBCursor cursor = cl.query(matcher, selector, orderBy, null);
            return new SequoiadbMetaCursor(metasource, db, cursor);
        }
        catch (Exception e) {
            releaseConnection(db);
            throw new ScmMetasourceException(
                    "query failed:csName=" + csName + ",clName=" + clName + ",matcher=" + matcher,
                    e);
        }
    }

    @Override
    public void ensureIndex(String indexName, BSONObject indexDefinition, boolean isUnique)
            throws ScmMetasourceException {
        Sequoiadb db = getConnection();
        try {
            DBCollection cl = SequoiadbHelper.getCL(db, csName, clName);
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
                cl.createIndex(indexName, indexDefinition, isUnique, false);
            }
            catch (Exception e) {
                throw new ScmMetasourceException(
                        "create index failed:csName=" + csName + ",clName=" + clName + ",indexName="
                                + indexName + ",indexDefinition=" + indexDefinition,
                        e);
            }
        }
        catch (ScmMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmMetasourceException("ensure index failed:csName=" + csName + ",clName="
                    + clName + ",indexName=" + indexName + ",indexDefinition=" + indexDefinition,
                    e);
        }
        finally {
            releaseConnection(db);
        }
    }
}
