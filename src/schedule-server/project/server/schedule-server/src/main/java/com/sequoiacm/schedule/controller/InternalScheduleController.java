package com.sequoiacm.schedule.controller;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.remote.ScheduleClient;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import com.sequoiacm.schedule.service.ScheduleService;

@RestController
@RequestMapping("/internal")
public class InternalScheduleController {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    @Autowired
    private ScheduleService service;
    @Autowired
    private ScheduleClientFactory clientFactory;

    private ScheduleFullEntity createSchedule2Leader(String leaderId, ScheduleUserEntity info)
            throws Exception {
        String leaderUrl = ScheduleCommonTools.joinInternalUrlElems(leaderId.split(":"));
        ScheduleClient client = clientFactory.getFeignClientByNodeUrl(leaderUrl);
        return client.createSchedule(ScheduleEntityTranslator.UserInfo.toJSONString(info));
    }

    @RequestMapping(value = "/v1/schedules", method = { RequestMethod.POST, RequestMethod.PUT })
    public ScheduleFullEntity createSchedule(HttpServletRequest request) throws Exception {
        ScheduleUserEntity info = ScheduleEntityTranslator.UserInfo.fromRequest(request);
        if (info.getType().equals(ScheduleDefine.ScheduleType.CLEAN_FILE)
                || info.getType().equals(ScheduleDefine.ScheduleType.CLEAN_FILE)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "can not create clean or copy file schedule: type=" + info.getType());
        }
        if (!ScheduleElector.getInstance().isLeader()) {
            String leaderId = ScheduleElector.getInstance().getLeader();
            if (isLeaderStillMe(leaderId, ScheduleElector.getInstance().getId())) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                        "I'm not leader yet!:id=" + leaderId);
            }

            if ("".equals(leaderId)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                        "there is not leader!:id=" + leaderId);
            }
            ScheduleFullEntity createSchedule2Leader = createSchedule2Leader(leaderId, info);
            return createSchedule2Leader;
        }
        else {
            ScheduleFullEntity createSchedule = service.createSchedule(null, info);
            return createSchedule;
        }
    }

    private void deleteSchedule2Leader(String leaderId, String scheduleId, boolean stopWorker) throws Exception {
        String targetUrl = ScheduleCommonTools.joinInternalUrlElems(leaderId.split(":"));
        ScheduleClient client = clientFactory.getFeignClientByNodeUrl(targetUrl);
        client.deleteSchdule(scheduleId, stopWorker);
    }

    @GetMapping("/v1/schedules/status/{schedule_name}")
    public InternalSchStatus getInternalSchLatestStatusV1(
            @PathVariable("schedule_name") String scheduleName) throws Exception {
        return service.getInternalSchLatestStatusByName(scheduleName);
    }


    // 新增的 v2 接口与 v1 版本实现一致，用于如下场景：
    // 老版本调度服务的 v1 接口存在 bug，其 isFinish 属性固定为 false
    // 客户端优先使用 v2 版本查询 status，若 v2 版本不存在，则使用 v1，并且客户端将会重新矫正 finish 属性
    @GetMapping("/v2/schedules/status/{schedule_name}")
    public InternalSchStatus getInternalSchLatestStatusV2(
            @PathVariable("schedule_name") String scheduleName) throws Exception {
        return service.getInternalSchLatestStatusByName(scheduleName);
    }

    @PostMapping("/v1/schedules/status/{schedule_id}")
    public void reportInternalSchStatus(@PathVariable("schedule_id") String scheduleId,
            @RequestParam(RestCommonDefine.RestParam.REST_WORKER_NODE) String workerNode,
            @RequestParam(RestCommonDefine.RestParam.REST_START_TIME) long startTime,
            @RequestParam(value = RestCommonDefine.RestParam.REST_WORKER_STATUS, required = false) BSONObject statusInfo,
            @RequestParam(RestCommonDefine.RestParam.REST_WORKER_IS_FINISH) boolean isFinish)
            throws Exception {
        InternalSchStatus status = new InternalSchStatus();
        status.setFinish(isFinish);
        status.setSchId(scheduleId);
        status.setStartTime(startTime);
        status.setStatus(statusInfo);
        status.setWorkerNode(workerNode);
        if (status.getStatus() != null) {
            service.reportInternalSchStatus(status);
        }
        if (status.isFinish()) {
            deleteSchedule(status.getSchId(), false);
        }
    }

    @DeleteMapping("/v1/schedules/{schedule_id}")
    public void deleteSchedule(@PathVariable("schedule_id") String scheduleId,
            @RequestParam(value = RestCommonDefine.RestParam.STOP_WORKER, required = false, defaultValue = "false") boolean stopWorker)
            throws Exception {
        if (!ScheduleElector.getInstance().isLeader()) {
            String leaderId = ScheduleElector.getInstance().getLeader();
            if (isLeaderStillMe(leaderId, ScheduleElector.getInstance().getId())) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                        "I'm not leader yet!:id=" + leaderId);
            }
            deleteSchedule2Leader(leaderId, scheduleId, stopWorker);
        }
        else {
            service.deleteSchedule(scheduleId, stopWorker);
        }
    }

    @GetMapping("/v1/schedules/{schedule_id}")
    public ScheduleFullEntity getSchedule(@PathVariable("schedule_id") String scheduleId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        ScheduleFullEntity info = service.getSchedule(scheduleId);
        return info;
    }

    @GetMapping("/v1/schedules")
    public void listSchedules(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String filter = request.getParameter(RestCommonDefine.RestParam.KEY_QUERY_FILTER);
        BSONObject condition = null;
        if (null != filter) {
            condition = (BSONObject) JSON.parse(filter);
        }
        else {
            condition = new BasicBSONObject();
        }

        ScmBSONObjectCursor cursor = null;
        try {
            cursor = service.listSchedule(condition);

            response.setHeader(RestCommonDefine.CONTENT_TYPE,
                    RestCommonDefine.APPLICATION_JSON_UTF8);
            PrintWriter writer = response.getWriter();

            writer.write("[");
            boolean isFirstObj = true;
            while (cursor.hasNext()) {
                BSONObject obj = cursor.next();
                if (!isFirstObj) {
                    writer.write(",");
                }
                else {
                    isFirstObj = false;
                }

                obj.removeField("_id");
                writer.write(obj.toString());
            }

            writer.write("]");
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    private boolean isLeaderStillMe(String leaderId, String myId) {
        if (leaderId.equals(myId)) {
            return true;
        }
        return false;
    }
}
