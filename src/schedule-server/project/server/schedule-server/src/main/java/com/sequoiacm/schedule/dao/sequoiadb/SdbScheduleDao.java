package com.sequoiacm.schedule.dao.sequoiadb;

import java.util.List;

import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.dao.SequoiadbTemplate;
import com.sequoiacm.schedule.dao.Transaction;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.model.ScheduleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.dao.ScheduleDao;
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

    private SequoiadbTemplate template;

    @Autowired
    public SdbScheduleDao(SdbDataSourceWrapper datasource) {
        this.datasource = datasource;
        this.template = new SequoiadbTemplate(datasource);
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
            if (e instanceof BaseException) {
                if (((BaseException) e).getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.SCHEDULE_EXISTS,
                            "schedule with the same name already exists:workspace="
                                    + info.getWorkspace() + ",name=" + info.getName());
                }
            }
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
    public ScmBSONObjectCursor query(BSONObject matcher, BSONObject orderBy, long skip, long limit)
            throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher, orderBy, skip, limit);
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
    public void update(BSONObject matcher, BSONObject updator) throws Exception {
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
    public void updateByScheduleId(String scheduleId, BSONObject newValue) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Schedule.FIELD_ID, scheduleId);
        BSONObject updator = new BasicBSONObject();
        updator.put("$set", newValue);
        try {
            update(matcher, updator);
        }
        catch (Exception e) {
            if (e instanceof BaseException) {
                if (((BaseException) e).getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                    throw new ScheduleException(RestCommonDefine.ErrorCode.SCHEDULE_EXISTS,
                            "schedule with the same name already exists,workspace="
                                    + queryOne(scheduleId).getWorkspace() + ",name="
                                    + newValue.get(FieldName.Schedule.FIELD_NAME));
                }
            }
            throw e;
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

    @Override
    public void delete(BSONObject matcher, Transaction t) throws Exception {
        template.collection(csName, clName).delete(matcher, (TransactionImpl) t);
    }

    @Override
    public long countSchedule(BSONObject condition) throws Exception {
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            return cl.getCount(condition);
        }
        catch (Exception e) {
            logger.error("get schedule count failed[cs={},cl={}]:condition={}", csName, clName,
                    condition);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

    @Override
    public ScheduleFullEntity queryOneByName(String name) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.Schedule.FIELD_NAME, name);
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, null, null, 0);
            if (null == result) {
                return null;
            }

            List<String> l = exec(String.class);

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

    public <T> List<T> exec(Class<T> c) {
        return null;
    }
}
