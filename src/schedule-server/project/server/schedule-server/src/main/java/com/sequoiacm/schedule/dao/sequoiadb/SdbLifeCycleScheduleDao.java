package com.sequoiacm.schedule.dao.sequoiadb;

import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.model.TransitionEntityTranslator;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.dao.LifeCycleScheduleDao;
import com.sequoiacm.schedule.dao.SequoiadbTemplate;
import com.sequoiacm.schedule.dao.Transaction;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("LifeCycleScheduleDao")
public class SdbLifeCycleScheduleDao implements LifeCycleScheduleDao {
    private static final Logger logger = LoggerFactory.getLogger(SdbLifeCycleScheduleDao.class);

    private String csName = "SCMSYSTEM";
    private String clName = "WORKSPACE_LIFE_CYCLE_CONFIG";

    private SdbDataSourceWrapper datasource;

    private SequoiadbTemplate template;

    @Autowired
    public SdbLifeCycleScheduleDao(SdbDataSourceWrapper datasource) throws Exception {
        this.datasource = datasource;
        this.template = new SequoiadbTemplate(datasource);
        SequoiadbTemplate.SequoiadbCollectionTemplate collection = template.collection(csName,
                clName);
        collection.ensureTable();
        collection.ensureIndex("idx_workspace_life_cycle_config_id", new BasicBSONObject("id", 1),
                true,
                false);
        collection.ensureIndex("idx_workspace_life_cycle_config_workspace",
                new BasicBSONObject("workspace", 1), false, false);
    }

    @Override
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher);
    }

    @Override
    public void update(BSONObject obj, Transaction t) throws Exception {
        String id = (String) obj.get(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID);
        obj.removeField(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID);
        BSONObject matcher = new BasicBSONObject(
                FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID, id);
        BSONObject modifier = new BasicBSONObject("$set", obj);

        template.collection(csName, clName).update(matcher, modifier, (TransactionImpl) t);
    }

    @Override
    public void insert(BSONObject obj, Transaction t) throws Exception {
        template.collection(csName, clName).insert(obj, (TransactionImpl) t);
    }

    @Override
    public TransitionScheduleEntity queryByName(String workspace, String transitionName)
            throws Exception {
        BSONObject workspaceBSON = new BasicBSONObject(
                FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME, workspace);
        BSONObject nameBson = new BasicBSONObject(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME,
                transitionName);

        BasicBSONList array = new BasicBSONList();
        array.add(workspaceBSON);
        array.add(nameBson);
        BSONObject matcher = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_AND, array);

        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, null, null, 0);
            if (null == result) {
                return null;
            }
            return TransitionEntityTranslator.WsFullInfo.fromBSONObject(result);
        }
        catch (Exception e) {
            logger.error("query workspace transition failed[cs={},cl={}]:matcher={}", csName,
                    clName, matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void delete(String id, Transaction t) throws Exception {
        BSONObject matcher = new BasicBSONObject(
                FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID, id);
        template.collection(csName, clName).delete(matcher, (TransactionImpl) t);
    }

    @Override
    public void delete(BSONObject matcher, Transaction t) throws Exception {
        template.collection(csName, clName).delete(matcher, (TransactionImpl) t);
    }

    @Override
    public TransitionScheduleEntity queryOne(BSONObject matcher) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, null, null, 0);
            if (null == result) {
                return null;
            }
            return TransitionEntityTranslator.WsFullInfo.fromBSONObject(result);
        }
        catch (Exception e) {
            logger.error("query workspace transition failed[cs={},cl={}]:matcher={}", csName,
                    clName, matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public ScmBSONObjectCursor query(BSONObject matcher, BSONObject orderBy) throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher, orderBy, 0, -1);
    }
}
