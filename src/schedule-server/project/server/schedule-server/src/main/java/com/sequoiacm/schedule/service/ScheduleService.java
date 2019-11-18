package com.sequoiacm.schedule.service;

import org.bson.BSONObject;

import com.sequoiacm.schedule.entity.ScheduleFullEntity;
import com.sequoiacm.schedule.entity.ScheduleNewUserInfo;
import com.sequoiacm.schedule.entity.ScheduleUserEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

public interface ScheduleService {
    public ScheduleFullEntity createSchedule(String createUser, ScheduleUserEntity info) throws Exception;

    public void deleteSchedule(String scheduleId) throws Exception;

    public ScheduleFullEntity getSchedule(String scheduleId) throws Exception;

    public ScmBSONObjectCursor listSchedule(BSONObject condition) throws Exception;

    public ScheduleFullEntity updateSchedule(String scheduleId, ScheduleNewUserInfo newInfo)
            throws Exception;
}
