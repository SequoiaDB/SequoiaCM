package com.sequoiacm.schedule.service;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.schedule.core.ScheduleMgrWrapper;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.entity.ScheduleFullEntity;
import com.sequoiacm.schedule.entity.ScheduleNewUserInfo;
import com.sequoiacm.schedule.entity.ScheduleUserEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    @Autowired
    private ScheduleDao dao;

    @Override
    public ScheduleFullEntity createSchedule(String createUser, ScheduleUserEntity info) throws Exception {
        return ScheduleMgrWrapper.getInstance().createSchedule(createUser, info);
    }

    @Override
    public void deleteSchedule(String scheduleId) throws Exception {
        ScheduleMgrWrapper.getInstance().deleteSchedule(scheduleId);
    }

    @Override
    public ScheduleFullEntity getSchedule(String scheduleId) throws Exception {
        return ScheduleMgrWrapper.getInstance().getSchedule(scheduleId);
    }

    @Override
    public ScmBSONObjectCursor listSchedule(BSONObject condition) throws Exception {
        return ScheduleMgrWrapper.getInstance().getSchedule(condition);
    }

    @Override
    public ScheduleFullEntity updateSchedule(String scheduleId, ScheduleNewUserInfo newInfo)
            throws Exception {
        return ScheduleMgrWrapper.getInstance().updateSchedule(scheduleId, newInfo);
    }
}
