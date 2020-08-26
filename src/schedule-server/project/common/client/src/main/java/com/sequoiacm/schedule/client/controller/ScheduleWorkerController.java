package com.sequoiacm.schedule.client.controller;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequoiacm.schedule.client.worker.ScheduleWorkerMgr;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;

@ResponseBody
@RequestMapping("/internal/v1")
public class ScheduleWorkerController {
    @Autowired
    private ScheduleWorkerMgr workerMgr;

    @PostMapping(value = "/schedules/worker", params = "action=start")
    public void startJob(@RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_ID) String schId,
            @RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_NAME) String schName,
            @RequestParam(RestCommonDefine.RestParam.REST_START_TIME) long startTime,
            @RequestParam(RestCommonDefine.RestParam.REST_JOB_TYPE) String jobType,
            @RequestParam(RestCommonDefine.RestParam.REST_JOB_DATA) BSONObject jobData)
            throws ScheduleException {
        workerMgr.startJob(schId, schName, startTime, jobType, jobData);
    }

    @PostMapping(value = "/schedules/worker", params = "action=stop")
    public void stopJob(@RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_ID) String schId)
            throws ScheduleException {
        workerMgr.stopJob(schId);
    }

    @GetMapping(value = "/schedules/worker/is_running")
    public boolean isJobRunning(
            @RequestParam(RestCommonDefine.RestParam.REST_SCHEDULE_ID) String schId)
            throws ScheduleException {
        return workerMgr.isJobRunning(schId);
    }

}
