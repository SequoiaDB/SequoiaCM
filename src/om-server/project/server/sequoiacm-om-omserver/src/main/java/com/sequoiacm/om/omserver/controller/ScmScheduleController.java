package com.sequoiacm.om.omserver.controller;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.module.*;
import com.sequoiacm.om.omserver.service.ScmScheduleService;
import com.sequoiacm.om.omserver.service.ScmTaskService;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@RestController
@RequestMapping("/api/v1")
public class ScmScheduleController {
    @Autowired
    private ScmScheduleService scheduleService;

    @Autowired
    private ScmTaskService taskService;

    @GetMapping("/schedules")
    public List<OmScheduleBasicInfo> listSchedule(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") long limit,
            @RequestParam(value = RestParamDefine.FILTER, required = false) BSONObject filter,
            @RequestParam(value = RestParamDefine.ORDERBY, required = false) BSONObject orderBy,
            HttpServletResponse response) throws ScmInternalException, ScmOmServerException {
        if (null == filter) {
            filter = new BasicBSONObject();
        }
        long scheduleCount = scheduleService.getScheduleCount(session, filter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(scheduleCount));
        if (scheduleCount <= 0) {
            return Collections.emptyList();
        }
        return scheduleService.getScheduleList(session, filter, orderBy, skip, limit);
    }

    @PutMapping("/schedules")
    public void createSchedule(ScmOmSession session,
            @RequestBody @Validated(OmScheduleInfo.CREATE.class) OmScheduleInfo omScheduleInfo)
            throws ScmInternalException, ScmOmServerException {
        if (null == omScheduleInfo.getEnable()) {
            omScheduleInfo.setEnable(true);
        }
        scheduleService.createSchedule(session, omScheduleInfo);
    }

    @PostMapping("/schedules/{schedule_id}")
    public void updateSchedule(ScmOmSession session, @PathVariable("schedule_id") String scheduleId,
            @RequestBody OmScheduleInfo omScheduleInfo)
            throws ScmInternalException, ScmOmServerException {
        omScheduleInfo.setScheduleId(scheduleId);
        scheduleService.updateSchedule(session, omScheduleInfo);
    }

    @GetMapping("/schedules/{schedule_id}")
    public OmScheduleInfo getScheduleDetail(ScmOmSession session,
            @PathVariable("schedule_id") String scheduleId)
            throws ScmOmServerException, ScmInternalException {
        return scheduleService.getScheduleDetail(session, scheduleId);
    }

    @DeleteMapping("/schedules/{schedule_id}")
    public void deleteSchedule(ScmOmSession session, @PathVariable("schedule_id") String scheduleId)
            throws ScmOmServerException, ScmInternalException {
        scheduleService.deleteSchedule(session, scheduleId);
    }

    @GetMapping("/schedules/tasks")
    public List<OmTaskBasicInfo> listTask(ScmOmSession session,
            @RequestParam(value = RestParamDefine.SCHEDULE_ID, required = true) String scheduleId,
            @RequestParam(value = RestParamDefine.FILTER, required = false) BSONObject filter,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") long limit,
            @RequestParam(value = RestParamDefine.ORDERBY, required = false) BSONObject orderBy,
            HttpServletResponse response) throws ScmInternalException, ScmOmServerException, ScmException {
        if (null == filter) {
            filter = new BasicBSONObject();
        }
        BSONObject taskCountFilter = null;
        taskCountFilter = ScmQueryBuilder.start().and(filter)
                .and(new BasicBSONObject(ScmAttributeName.Task.SCHEDULE_ID, scheduleId)).get();
        long taskCount = taskService.getTaskCount(session, taskCountFilter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(taskCount));
        if (taskCount <= 0) {
            return Collections.emptyList();
        }
        return scheduleService.getScheduleTasks(session, scheduleId, filter, orderBy, skip, limit);
    }

}
