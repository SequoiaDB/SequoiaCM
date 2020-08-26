package com.sequoiacm.schedule.client.feign;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;

@RequestMapping("/internal/v1")
public interface ScheduleFeignClient {

    @PostMapping(value = "/schedules")
    public ScheduleFullEntity createSchedule(
            @RequestParam(RestCommonDefine.RestParam.KEY_DESCRIPTION) String schJson)
            throws ScheduleException;

    @DeleteMapping("/schedules/{schedule_id}")
    public void deleteSchedule(@PathVariable("schedule_id") String schId,
            @RequestParam(RestCommonDefine.RestParam.STOP_WORKER) boolean stopWorker)
            throws ScheduleException;

    @GetMapping("/schedules")
    public List<ScheduleFullEntity> listSchedule(
            @RequestParam(RestCommonDefine.RestParam.KEY_QUERY_FILTER) String filter)
            throws ScheduleException;

    @PostMapping("/schedules/status/{schedule_id}")
    public List<ScheduleFullEntity> reportInternalSchStatus(
            @PathVariable("schedule_id") String scheduleId,
            @RequestParam(RestCommonDefine.RestParam.REST_WORKER_NODE) String workerNode,
            @RequestParam(RestCommonDefine.RestParam.REST_START_TIME) long startTime,
            @RequestParam(RestCommonDefine.RestParam.REST_WORKER_STATUS) BSONObject innerStatus,
            @RequestParam(RestCommonDefine.RestParam.REST_WORKER_IS_FINISH) boolean isFinish)
            throws ScheduleException;

    @GetMapping("/schedules/status/{schedule_name}")
    public InternalSchStatus getInternalSchLatestStatus(
            @PathVariable("schedule_name") String scheduleName) throws ScheduleException;
}
