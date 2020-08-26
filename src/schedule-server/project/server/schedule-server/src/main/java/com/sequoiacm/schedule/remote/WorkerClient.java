package com.sequoiacm.schedule.remote;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;

@RequestMapping("/internal/v1")
public interface WorkerClient {
    @PostMapping(value = "/schedules/worker?action=start")
    public void startJob(
            @RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_ID) String scheduleId,
            @RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_NAME) String schedulename,
            @RequestParam(RestCommonDefine.RestParam.REST_START_TIME) long startTime,
            @RequestParam(RestCommonDefine.RestParam.REST_JOB_TYPE) String jobType,
            @RequestParam(RestCommonDefine.RestParam.REST_JOB_DATA) BSONObject jobData)
            throws ScheduleException;

    @PostMapping(value = "/schedules/worker?action=stop")
    public void stopJob(
            @RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_ID) String scheduleId)
            throws ScheduleException;

    @GetMapping(value = "/schedules/worker/is_running")
    public boolean isJobRunning(
            @RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_ID) String scheduleId)
            throws ScheduleException;
}
