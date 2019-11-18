package com.sequoiacm.schedule.core;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.core.job.CleanJobInfo;
import com.sequoiacm.schedule.core.job.CopyJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleMgr;
import com.sequoiacm.schedule.core.job.quartz.QuartzScheduleMgr;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.entity.ScheduleEntityTranslator;
import com.sequoiacm.schedule.entity.ScheduleFullEntity;
import com.sequoiacm.schedule.entity.ScheduleNewUserInfo;
import com.sequoiacm.schedule.entity.ScheduleUserEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.exception.ScheduleException;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;

public class ScheduleMgrWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleMgrWrapper.class);

    private static ScheduleMgrWrapper instance = new ScheduleMgrWrapper();

    private ScheduleDao scheduleDao;

    Lock lock = new ReentrantLock();

    private ScheduleMgr mgr;

    private ScheduleClientFactory clientFactory;

    private ScheduleMgrWrapper() {
    }

    public static ScheduleMgrWrapper getInstance() {
        return instance;
    }

    public void init(ScheduleDao scheduleDao, ScheduleClientFactory clientFactory) {
        this.scheduleDao = scheduleDao;
        this.clientFactory = clientFactory;
    }

    public void start() throws Exception {
        lock.lock();
        try {
            if (null != mgr) {
                return;
            }

            mgr = new QuartzScheduleMgr();
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
                    info.getContent(), info.getCron());
        }
        else if (info.getType().equals(ScheduleDefine.ScheduleType.COPY_FILE)) {
            jobInfo = new CopyJobInfo(info.getId(), info.getType(), info.getWorkspace(),
                    info.getContent(), info.getCron());
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

            if (info.isEnable()) {
                mgr.createJob(jobInfo);
            }
            scheduleDao.insert(info);
            logger.info("create schedule job sucess:id={},enable={}", jobInfo.getId(),
                    info.isEnable());
            return info;
        }
        catch (Exception e) {
            logger.info("deleting schedule job:id={},enable={}", info.getId(), info.isEnable());
            deleteJobSilence(info.getId());
            throw e;
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
    private void deleteJobSilence(String jobId) {
        try {
            if (null == mgr) {
                return;
            }

            if (mgr.getJobInfo(jobId) != null) {
                mgr.deleteJob(jobId);
            }
        }
        catch (Exception e) {
            logger.warn("delete job failed:jobId={}", jobId, e);
        }
    }

    public void deleteSchedule(String scheduleId) throws Exception {
        lock.lock();
        try {
            if (null == mgr) {
                throw new Exception("mgr is null");
            }

            scheduleDao.delete(scheduleId);
            deleteJobSilence(scheduleId);
            logger.info("delete schedule job sucess:id={}", scheduleId);
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
                    deleteJobSilence(createdJob.getId());
                }
            }

            // remove schedule from db
            BSONObject wsMatcher = new BasicBSONObject(FieldName.Schedule.FIELD_WORKSPACE, wsName);
            scheduleDao.delete(wsMatcher);
            logger.info("delete schedules sucess:ws={}", wsName);
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

    public ScmBSONObjectCursor getSchedule(BSONObject condition) throws Exception {
        return scheduleDao.query(condition);
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
                mgr.deleteJob(scheduleId);
                isOldJobDeleted = true;
            }

            if (newFullInfo.isEnable()) {
                // 2.create new
                ScheduleJobInfo newJobInfo = createJobInfo(newFullInfo);
                mgr.createJob(newJobInfo);
                isNewJobCreated = true;
            }

            // 3.write to db
            scheduleDao.update(scheduleId, newValue);

            logger.info("update schedule job sucess:id={}", scheduleId);

            return newFullInfo;
        }
        catch (Exception e) {
            logger.error("update schedule failed:scheduleId={}", scheduleId, e);

            // restore the old job
            try {
                if (isNewJobCreated) {
                    mgr.deleteJob(scheduleId);
                }

                if (isOldJobDeleted) {
                    mgr.createJob(oldJobInfo);
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

        ScheduleJobInfo jobInfo;
        try {
            jobInfo = createJobInfo(info);
        }
        catch (Exception e) {
            logger.warn("schedule info contain some invalid arguments, disable this schedule:{}",
                    info, e);
            disableScheduleSilence(info);
            return;
        }

        mgr.createJob(jobInfo);

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
