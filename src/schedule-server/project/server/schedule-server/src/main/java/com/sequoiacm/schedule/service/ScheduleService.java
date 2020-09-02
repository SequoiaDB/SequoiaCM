package com.sequoiacm.schedule.service;

import org.bson.BSONObject;

import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleNewUserInfo;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

public interface ScheduleService {
    public ScheduleFullEntity createSchedule(String createUser, ScheduleUserEntity info)
            throws Exception;

    public void deleteSchedule(String scheduleId, boolean stopWorker) throws Exception;

    public ScheduleFullEntity getSchedule(String scheduleId) throws Exception;

    public ScmBSONObjectCursor listSchedule(BSONObject condition) throws Exception;

    public ScheduleFullEntity updateSchedule(String scheduleId, ScheduleNewUserInfo newInfo)
            throws Exception;

    public InternalSchStatus getInternalSchLatestStatusByName(String scheduleName) throws Exception;

    public void reportInternalSchStatus(InternalSchStatus status) throws Exception;
}
