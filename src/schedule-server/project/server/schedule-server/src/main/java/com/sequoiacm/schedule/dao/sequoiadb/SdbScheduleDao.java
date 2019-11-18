package com.sequoiacm.schedule.dao.sequoiadb;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.entity.ScheduleEntityTranslator;
import com.sequoiacm.schedule.entity.ScheduleFullEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

@Repository("ScheduleDao")
public class SdbScheduleDao implements ScheduleDao {
    private static final Logger logger = LoggerFactory.getLogger(SdbScheduleDao.class);

    SdbDataSourceWrapper datasource;
    private String csName = "SCMSYSTEM";
    private String clName = "SCHEDULE";

    @Autowired
    public SdbScheduleDao(SdbDataSourceWrapper datasource) throws Exception {
        this.datasource = datasource;
    }

    @Override
    public void insert(ScheduleFullEntity info) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject obj = ScheduleEntityTranslator.FullInfo.toBSONObject(info);
            cl.insert(obj);
        }
        catch (Exception e) {
            logger.error("insert schedule failed[cs={},cl={}]:info={}", csName, clName, info);
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
    public ScheduleFullEntity queryOne(String scheduleId) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Schedule.FIELD_ID, scheduleId);
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, null, null, 0);
            if (null == result) {
                return null;
            }

            return ScheduleEntityTranslator.FullInfo.fromBSONObject(result);
        }
        catch (Exception e) {
            logger.error("query schedule failed[cs={},cl={}]:matcher={}", csName, clName, matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void delete(String scheduleId) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Schedule.FIELD_ID, scheduleId);
        delete(matcher);
    }

    @Override
    public void update(String scheduleId, BSONObject newValue) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Schedule.FIELD_ID, scheduleId);
        BSONObject updator = new BasicBSONObject();
        updator.put("$set", newValue);
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cl.update(matcher, updator, null);
        }
        catch (Exception e) {
            logger.error("update schedule failed[cs={},cl={}]:matcher={},updator={}", csName,
                    clName, matcher, updator);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public void delete(BSONObject matcher) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cl.delete(matcher);
        }
        catch (Exception e) {
            logger.error("delete schedule failed[cs={},cl={}]:matcher={}", csName, clName, matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

}
