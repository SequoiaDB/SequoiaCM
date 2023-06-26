package com.sequoiacm.schedule.dao.sequoiadb;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.schedule.dao.ScheduleDao;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleEntityTranslator;
import com.sequoiacm.schedule.dao.InternalSchStatusDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

@Repository
public class SdbInternalSchStatusDao implements InternalSchStatusDao {
    private static final Logger logger = LoggerFactory.getLogger(SdbInternalSchStatusDao.class);
    private SdbDataSourceWrapper datasource;

    @Autowired
    private ScheduleDao scheduleDao;

    private String csName = "SCMSYSTEM";
    private String clName = "SCHEDULE_STATUS";

    @Autowired
    public SdbInternalSchStatusDao(SdbDataSourceWrapper datasource) throws Exception {
        this.datasource = datasource;
    }

    @Override
    public List<InternalSchStatus> getStatusById(String id) throws Exception {
        List<InternalSchStatus> ret = new ArrayList<>();
        ScmBSONObjectCursor c = SdbDaoCommon.query(datasource, csName, clName,
                new BasicBSONObject(FieldName.ScheduleStatus.FIELD_ID, id));
        try {
            while (c.hasNext()) {
                ret.add(ScheduleEntityTranslator.Status.fromBSON(c.next()));
            }
        }
        finally {
            c.close();
        }
        return ret;
    }

    @Override
    public InternalSchStatus getLatestStatusByName(String name) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.ScheduleStatus.FIELD_NAME, name);
        BSONObject orderby = new BasicBSONObject(FieldName.ScheduleStatus.FIELD_START_TIME, -1);
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, orderby, null, 0);
            if (null == result) {
                return null;
            }

            if (!result.containsField(FieldName.ScheduleStatus.FIELD_IS_FINISH)) {
                // 老版本调度服务写入 status 时遗漏了 isFinish，这里补上去（finish 为 true 时，调度任务不存在）
                result.put(FieldName.ScheduleStatus.FIELD_IS_FINISH,
                        scheduleDao.queryOneByName(name) == null);
            }

            return ScheduleEntityTranslator.Status.fromBSON(result);
        }
        catch (Exception e) {
            logger.error("query contentserver failed[cs={},cl={}]:matcher={}", csName, clName,
                    matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }

    }

    @Override
    public void upsertStatus(InternalSchStatus status) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.ScheduleStatus.FIELD_ID,
                status.getSchId());
        matcher.put(FieldName.ScheduleStatus.FIELD_START_TIME, status.getStartTime());
        matcher.put(FieldName.ScheduleStatus.FIELD_WORKER_NODE, status.getWorkerNode());

        BSONObject updator = new BasicBSONObject("$set",
                ScheduleEntityTranslator.Status.toBSON(status));

        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            cl.upsert(matcher, updator, null);
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }

}
