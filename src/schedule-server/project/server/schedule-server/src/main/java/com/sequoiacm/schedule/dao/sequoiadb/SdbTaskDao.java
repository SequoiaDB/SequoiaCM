package com.sequoiacm.schedule.dao.sequoiadb;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.dao.TaskDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.entity.TaskEntity;
import com.sequoiacm.schedule.entity.TaskEntityTranslator;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

@Repository("TaskDao")
public class SdbTaskDao implements TaskDao {
    private static final Logger logger = LoggerFactory.getLogger(SdbTaskDao.class);

    private SdbDataSourceWrapper datasource;
    private String csName = "SCMSYSTEM";
    private String clName = "TASK";

    @Autowired
    public SdbTaskDao(SdbDataSourceWrapper datasource) {
        this.datasource = datasource;
    }

    @Override
    public void insert(TaskEntity info) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject obj = TaskEntityTranslator.toBSONObject(info);
            cl.insert(obj);
        }
        catch (Exception e) {
            logger.error("insert task failed[cs={},cl={}]:info={}", csName, clName, info);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher);
    }

    @Override
    public TaskEntity queryOne(String taskId) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, null, null, 0);
            if (null == result) {
                return null;
            }

            return TaskEntityTranslator.fromBSONObject(result);
        }
        catch (Exception e) {
            logger.error("query task failed[cs={},cl={}]:matcher={}", csName, clName, matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void delete(String taskId) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject obj = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            cl.delete(obj);
        }
        catch (Exception e) {
            logger.error("delete task failed[cs={},cl={}]:task={}", csName, clName, taskId);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }
}
