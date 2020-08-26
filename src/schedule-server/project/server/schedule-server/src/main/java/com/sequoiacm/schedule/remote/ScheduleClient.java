package com.sequoiacm.schedule.remote;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;

public interface ScheduleClient {

    @PostMapping(value = "/schedules")
    public ScheduleFullEntity createSchedule(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestParam(RestCommonDefine.RestParam.KEY_DESCRIPTION) String userEntityJson)
                    throws ScheduleException;

    @DeleteMapping(value = "/schedules/{schedule_id}")
    public void deleteSchdule(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("schedule_id") String scheduleId) throws ScheduleException;

    @PutMapping(value = "/schedules/{schedule_id}")
    public ScheduleFullEntity updateSchdule(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("schedule_id") String scheduleId,
            @RequestParam(RestCommonDefine.RestParam.KEY_DESCRIPTION) String description)
                    throws ScheduleException;

    @PostMapping(value = "/tasks/{taskId}/notify")
    public void notifyTask(@PathVariable("taskId") String taskId,
            @RequestParam(RestCommonDefine.RestParam.KEY_NOTIFY_TYPE) int notifyType);
}
