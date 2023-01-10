package com.sequoiacm.schedule.dao.sequoiadb;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.model.LifeCycleConfigFullEntity;
import com.sequoiacm.schedule.common.model.LifeCycleEntityTranslator;
import com.sequoiacm.schedule.dao.LifeCycleConfigDao;
import com.sequoiacm.schedule.dao.SequoiadbTemplate;
import com.sequoiacm.schedule.dao.Transaction;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


@Repository("LifeCycleConfigDao")
public class SdbLifeCycleConfigDao implements LifeCycleConfigDao {

    private static final Logger logger = LoggerFactory.getLogger(SdbLifeCycleConfigDao.class);

    private String csName = "SCMSYSTEM";
    private String clName = "GLOBAL_LIFE_CYCLE_CONFIG";

    private SdbDataSourceWrapper datasource;

    private SequoiadbTemplate template;

    @Autowired
    public SdbLifeCycleConfigDao(SdbDataSourceWrapper datasource) throws Exception {
        this.datasource = datasource;
        this.template = new SequoiadbTemplate(datasource);
        template.collection(csName, clName).ensureTable();
    }

    @Override
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher);
    }

    @Override
    public BSONObject queryOne() throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(new BasicBSONObject(), null, null, null, 0);

            return result;
        }
        catch (Exception e) {
            logger.error("query schedule failed[cs={},cl={}]", csName, clName);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void insert(LifeCycleConfigFullEntity info) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject obj = LifeCycleEntityTranslator.FullInfo.toBSONObject(info);
            cl.insert(obj);
        }
        catch (Exception e) {
            logger.error("insert lifeCycleConfig failed[cs={},cl={}]:info={}", csName, clName,
                    info);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void update(BSONObject obj) throws Exception {
        String id = (String) obj.get(FieldName.LifeCycleConfig.FIELD_ID);
        BSONObject matcher = new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_ID,
                id);

        obj.removeField(FieldName.LifeCycleConfig.FIELD_ID);
        BSONObject updator = new BasicBSONObject("$set", obj);
        update(matcher, updator);
    }

    @Override
    public void update(BSONObject matcher, BSONObject updator) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cl.update(matcher, updator, null);
        }
        catch (Exception e) {
            logger.error("update life cycle config failed");
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void update(BSONObject obj, Transaction t) throws Exception {
        BSONObject modifier = new BasicBSONObject("$set", obj);

        template.collection(csName, clName).update(new BasicBSONObject(), modifier,
                (TransactionImpl) t);
    }

    @Override
    public void delete() throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cl.delete(new BasicBSONObject());
        }
        catch (Exception e) {
            logger.error("delete life cycle config failed");
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void delete(Transaction t) throws Exception {
        template.collection(csName, clName).delete(new BasicBSONObject(), (TransactionImpl) t);
    }

    @Override
    public void insert(BSONObject obj, Transaction t) throws Exception {
        template.collection(csName, clName).insert(obj, (TransactionImpl) t);
    }
}
