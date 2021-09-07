package com.sequoiacm.schedule.core;

import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.schedule.ScheduleApplicationConfig;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.*;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.core.job.*;
import com.sequoiacm.schedule.core.job.quartz.QuartzScheduleMgr;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ScheduleMgrWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleMgrWrapper.class);

    private static ScheduleMgrWrapper instance = new ScheduleMgrWrapper();

    private ScheduleDao scheduleDao;

    Lock lock = new ReentrantLock();

    private ScheduleMgr mgr;

    private ScheduleClientFactory clientFactory;

    private ScheduleApplicationConfig config;

    private ScmServiceDiscoveryClient discoveryClient;

    private ScheduleMgrWrapper() {
    }

    public static ScheduleMgrWrapper getInstance() {
        return instance;
    }

    public void init(ScheduleDao scheduleDao, ScheduleClientFactory clientFactory,
            ScheduleApplicationConfig config, ScmServiceDiscoveryClient discoveryClient) {
        this.scheduleDao = scheduleDao;
        this.clientFactory = clientFactory;
        this.config = config;
        this.discoveryClient = discoveryClient;
    }

    public void start() throws Exception {
        lock.lock();
        try {
            if (null != mgr) {
                return;
            }

            mgr = new QuartzScheduleMgr(config, clientFactory, discoveryClient, scheduleDao);
            ScmBSONObjectCursor cursor = null;
            try {
                cursor = scheduleDao.query(new BasicBSONObject());
                while (cursor.hasNext()) {
                    BSONObject obj = cursor.next();
                    restoreJob(obj);
                }
            }
            finally {
                if (null != cursor) {
                    cursor.close();
                }
            }

            mgr.start();
        }
        catch (Exception e) {
            clearWithoutLock();
            throw e;
        }
        finally {
            lock.unlock();
        }
    }

    private ScheduleJobInfo createJobInfo(ScheduleFullEntity info) throws ScheduleException {
        ScheduleJobInfo jobInfo = null;
        if (info.getType().equals(ScheduleDefine.ScheduleType.CLEAN_FILE)) {
            jobInfo = new CleanJobInfo(info.getId(), info.getType(), info.getWorkspace(),
                    info.getContent(), info.getCron(), info.getPreferredRegion(),
                    info.getPreferredZone());
        }
        else if (info.getType().equals(ScheduleDefine.ScheduleType.COPY_FILE)) {
            jobInfo = new CopyJobInfo(info.getId(), info.getType(), info.getWorkspace(),
                    info.getContent(), info.getCron(), info.getPreferredRegion(),
                    info.getPreferredZone());
        }
        else if (info.getType().equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            jobInfo = new InternalScheduleInfo(info.getId(), info.getName(), info.getType(),
                    info.getWorkspace(), info.getContent(), info.getCron(),
                    info.getPreferredRegion(), info.getPreferredZone());
        }
        else {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "schedule type is valid:type=" + info.getType());
        }

        return jobInfo;
    }

    public ScheduleFullEntity createSchedule(String user, ScheduleUserEntity userInfo)
            throws Exception {
        Date date = new Date();
        String id = ScmIdGenerator.ScheduleId.get(date);

        ScheduleFullEntity info = ScheduleEntityTranslator.FullInfo.fromUserInfo(userInfo, id, user,
                date.getTime());
        ScheduleJobInfo jobInfo = createJobInfo(info);

        lock.lock();
        try {
            if (null == mgr) {
                throw new Exception("mgr is null");
            }

            SchJobCreateContext context = mgr.prepareCreateJob(jobInfo);

            // 先入库，防止任务跑起来找不到记录
            scheduleDao.insert(info);

            if (info.isEnable()) {
                try {
                    mgr.createJob(context);
                }
                catch (Exception e) {
                    logger.error("failed to create schedule job, revote now:{}", jobInfo, e);
                    revote();
                    return info;
                }
            }
            logger.info("create schedule job success:id={},enable={}", jobInfo.getId(),
                    info.isEnable());
            return info;
        }
        finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            clearWithoutLock();
        }
        finally {
            lock.unlock();
        }
    }

    private void clearWithoutLock() {
        if (null != mgr) {
            mgr.clear();
            mgr = null;
        }
    }

    // must be in lock scope
    private void deleteJobSilence(String jobId, boolean stopWorker) {
        try {
            if (null == mgr) {
                return;
            }

            mgr.deleteJob(jobId, stopWorker);
        }
        catch (Exception e) {
            logger.warn("delete job failed:jobId={}", jobId, e);
        }
    }

    public void deleteSchedule(String scheduleId, boolean stopWorker) throws Exception {
        lock.lock();
        try {
            if (null == mgr) {
                throw new Exception("mgr is null");
            }

            scheduleDao.delete(scheduleId);
            deleteJobSilence(scheduleId, stopWorker);
            logger.info("delete schedule job success:id={}", scheduleId);
        }
        catch (Exception e) {
            logger.error("delete schedule failed:scheduleId={}", scheduleId);
            throw e;
        }
        finally {
            lock.unlock();
        }
    }

    public void deleteScheduleByWorkspace(String wsName) {
        lock.lock();
        try {
            if (null == mgr) {
                // i am not leader, just return
                return;
            }

            // remove schedule from ScheduleMgr
            for (ScheduleJobInfo createdJob : mgr.ListJob()) {
                if (createdJob.getWorkspace().equals(wsName)) {
                    deleteJobSilence(createdJob.getId(), false);
                }
            }

            // remove schedule from db
            BSONObject wsMatcher = new BasicBSONObject(FieldName.Schedule.FIELD_WORKSPACE, wsName);
            scheduleDao.delete(wsMatcher);
            logger.info("delete schedules success:ws={}", wsName);
        }
        catch (Exception e) {
            logger.error("delete schedules failed:ws={}", wsName, e);
        }
        finally {
            lock.unlock();
        }
    }

    public ScheduleFullEntity getSchedule(String scheduleId) throws Exception {
        ScheduleFullEntity info = scheduleDao.queryOne(scheduleId);
        if (null == info) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                    "schedule is not exist:schedule_id=" + scheduleId);
        }

        return info;
    }

    public ScheduleFullEntity getScheduleByName(String name) throws Exception {
        ScheduleFullEntity info = scheduleDao.queryOneByName(name);
        return info;
    }

    public ScmBSONObjectCursor getSchedule(BSONObject condition) throws Exception {
        return scheduleDao.query(condition, null, 0, -1);
    }

    public ScmBSONObjectCursor getSchedule(BSONObject condition, BSONObject orderBy, long skip,
            long limit) throws Exception {
        return scheduleDao.query(condition, orderBy, skip, limit);
    }

    public ScheduleFullEntity updateSchedule(String scheduleId, ScheduleNewUserInfo newInfo)
            throws Exception {
        ScheduleJobInfo oldJobInfo = null;
        boolean isOldJobDeleted = false;
        boolean isNewJobCreated = false;
        lock.lock();
        try {
            if (null == mgr) {
                throw new Exception("mgr is null");
            }

            ScheduleFullEntity oldFullInfo = scheduleDao.queryOne(scheduleId);
            if (null == oldFullInfo) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                        "schedule is not exit:scheduleId=" + scheduleId);
            }

            BSONObject newValue = new BasicBSONObject();
            ScheduleFullEntity newFullInfo = updateInfo(oldFullInfo, newInfo, newValue);

            oldJobInfo = mgr.getJobInfo(scheduleId);
            if (oldJobInfo != null) {
                // 1.delete old
                mgr.deleteJob(scheduleId, false);
                isOldJobDeleted = true;
            }

            if (newFullInfo.isEnable()) {
                // 2.create new
                ScheduleJobInfo newJobInfo = createJobInfo(newFullInfo);
                mgr.createJob(mgr.prepareCreateJob(newJobInfo));
                isNewJobCreated = true;
            }

            // 3.write to db
            scheduleDao.update(scheduleId, newValue);

            logger.info("update schedule job success:id={}", scheduleId);

            return newFullInfo;
        }
        catch (Exception e) {
            logger.error("update schedule failed:scheduleId={}", scheduleId, e);

            // restore the old job
            try {
                if (isNewJobCreated) {
                    mgr.deleteJob(scheduleId, false);
                }

                if (isOldJobDeleted) {
                    mgr.createJob(mgr.prepareCreateJob(oldJobInfo));
                }
            }
            catch (Exception e2) {
                logger.warn("restore schedule failed:oldJobInfo={}", oldJobInfo, e2);
                revote();
            }

            throw e;
        }
        finally {
            lock.unlock();
        }
    }

    private void revote() {
        try {
            ScheduleElector.getInstance().quitAndReVote();
        }
        catch (Exception e) {
            logger.error("revote failed", e);
            ScheduleCommonTools.exitProcess();
        }
    }

    private ScheduleFullEntity updateInfo(ScheduleFullEntity oldFullInfo,
            ScheduleNewUserInfo newInfo, BSONObject newValue) {
        ScheduleFullEntity newFullInfo = oldFullInfo.clone();

        if (null != newInfo.getContent()) {
            newFullInfo.setContent(newInfo.getContent());
            newValue.put(FieldName.Schedule.FIELD_CONTENT, newInfo.getContent());
        }

        if (null != newInfo.getCron()) {
            newFullInfo.setCron(newInfo.getCron());
            newValue.put(FieldName.Schedule.FIELD_CRON, newInfo.getCron());
        }

        if (null != newInfo.getDesc()) {
            newFullInfo.setDesc(newInfo.getDesc());
            newValue.put(FieldName.Schedule.FIELD_DESC, newInfo.getDesc());
        }

        if (null != newInfo.getName()) {
            newFullInfo.setName(newInfo.getName());
            newValue.put(FieldName.Schedule.FIELD_NAME, newInfo.getName());
        }

        if (null != newInfo.getType()) {
            newFullInfo.setType(newInfo.getType());
            newValue.put(FieldName.Schedule.FIELD_TYPE, newInfo.getType());
        }

        if (null != newInfo.getWorkspace()) {
            newFullInfo.setWorkspace(newInfo.getWorkspace());
            newValue.put(FieldName.Schedule.FIELD_WORKSPACE, newInfo.getWorkspace());
        }

        if (null != newInfo.isEnable()) {
            newFullInfo.setEnable(newInfo.isEnable());
            newValue.put(FieldName.Schedule.FIELD_ENABLE, newInfo.isEnable());
        }

        if (null != newInfo.getPreferredRegion()) {
            newFullInfo.setPreferredRegion(newInfo.getPreferredRegion());
            newValue.put(FieldName.Schedule.FIELD_PREFERRED_REGION, newInfo.getPreferredRegion());
        }

        if (null != newInfo.getPreferredZone()) {
            newFullInfo.setPreferredZone(newInfo.getPreferredZone());
            newValue.put(FieldName.Schedule.FIELD_PREFERRED_ZONE, newInfo.getPreferredZone());
        }

        return newFullInfo;
    }

    public ScheduleClientFactory getClientFactory() {
        return clientFactory;
    }

    private void restoreJob(BSONObject scheduleRecord) throws Exception {
        ScheduleFullEntity info;
        try {
            info = ScheduleEntityTranslator.FullInfo.fromBSONObject(scheduleRecord);
        }
        catch (Exception e) {
            logger.warn("unrecognized schedule record:{}", scheduleRecord, e);
            return;
        }

        if (!info.isEnable()) {
            return;
        }

        SchJobCreateContext context = null;
        try {
            ScheduleJobInfo jobInfo = createJobInfo(info);
            context = mgr.prepareCreateJob(jobInfo);
        }
        catch (Exception e) {
            logger.warn("schedule info contain some invalid arguments, disable this schedule:{}",
                    info, e);
            disableScheduleSilence(info);
            return;
        }
        mgr.createJob(context);
    }

    private void disableScheduleSilence(ScheduleFullEntity info) {
        try {
            BasicBSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Schedule.FIELD_ENABLE, false);
            scheduleDao.update(info.getId(), newValue);
        }
        catch (Exception e) {
            logger.warn("failed to disable schedule:id={}", info.getId(), e);
        }
    }
}
