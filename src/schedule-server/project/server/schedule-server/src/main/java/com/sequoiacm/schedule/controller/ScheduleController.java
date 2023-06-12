package com.sequoiacm.schedule.controller;

import java.io.PrintWriter;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.schedule.common.RestCommonField;
import com.sequoiacm.schedule.common.model.ScheduleClientEntity;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.dao.LifeCycleScheduleDao;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.SecurityRestField;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmWorkspaceResource;
import com.sequoiacm.schedule.ScheduleApplicationConfig;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleNewUserInfo;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.privilege.ScmSchedulePriv;
import com.sequoiacm.schedule.remote.ScheduleClient;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import com.sequoiacm.schedule.service.ScheduleService;

@RestController
@RequestMapping("/api/v1")
public class ScheduleController {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    @Autowired
    private ScheduleService service;
    @Autowired
    private ScheduleClientFactory clientFactory;
    @Autowired
    ScheduleApplicationConfig config;

    @Autowired
    private LifeCycleScheduleDao lifeCycleScheduleDao;

    @Autowired
    private ScmAudit audit;

    @RequestMapping(value = "/name", method = RequestMethod.HEAD)
    public ResponseEntity<String> getName(HttpServletRequest request, HttpServletResponse response,
            Authentication auth) {
        String name = request.getHeader("name");
        audit.info(ScmAuditType.SCHEDULE_DQL, auth, null, 0, "get schedule name, scheduleName=" + name);
        logger.info("name=" + name);
        response.setHeader("name", name);
        return ResponseEntity.ok("");
    }

    private ScheduleFullEntity createSchedule2Leader(String leaderId, ScheduleUserEntity info,
            String sessionId, String userDetail) throws Exception {
        String leaderUrl = ScheduleCommonTools.joinUrlElems(leaderId.split(":"));
        ScheduleClient client = clientFactory.getFeignClientByNodeUrl(leaderUrl);
        return client.createSchedule(sessionId, userDetail,
                ScheduleEntityTranslator.UserInfo.toJSONString(info));
    }

    void checkWsPriority(String userName, String wsName, ScmPrivilegeDefine op, String opDesc)
            throws ScheduleException {
        if (ScheduleServer.getInstance().getWorkspace(wsName) == null) {
            logger.error("workspace is not exist:ws={}", wsName);
            throw new ScheduleException(RestCommonDefine.ErrorCode.WORKSPACE_NOT_EXISTS,
                    opDesc + " failed, workspace is not exist:ws=" + wsName);
        }
        if (!ScmSchedulePriv.getInstance().hasPriority(userName, ScmWorkspaceResource.RESOURCE_TYPE,
                wsName, op)) {
            logger.error("do not have priority to {}:user={},ws={}", opDesc, userName, wsName);
            throw new ScheduleException(RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                    opDesc + " failed, do not have priority:user=" + userName + ",ws=" + wsName);
        }
    }

    @RequestMapping(value = "/schedules", method = { RequestMethod.POST, RequestMethod.PUT })
    public ScheduleFullEntity createSchedule(
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth,
            HttpServletRequest request) throws Exception {
        ScheduleUserEntity info = ScheduleEntityTranslator.UserInfo.fromRequest(request);
        checkWsPriority(auth.getName(), info.getWorkspace(), ScmPrivilegeDefine.CREATE,
                "create schedule");
        String userName = auth.getName();
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
            ScheduleFullEntity createSchedule2Leader = createSchedule2Leader(leaderId, info,
                    sessionId, userDetail);

            audit.info(ScmAuditType.CREATE_SCHEDULE, auth, info.getWorkspace(), 0,
                    "create schedule to leader, leaderId=" + leaderId + ", Schedule info: scheduleName="
                            + info.getName() + ", type=" + info.getType() + ", desc="
                            + info.getDesc());
            return createSchedule2Leader;
        }
        else {
            ScheduleFullEntity createSchedule = service.createSchedule(userName, info);

            audit.info(ScmAuditType.CREATE_SCHEDULE, auth, info.getWorkspace(), 0,
                    "create schedule, Schedule info: scheduleName=" + info.getName() + ", type="
                            + info.getType() + ", desc=" + info.getDesc());
            return createSchedule;
        }
    }

    private void deleteSchedule2Leader(String leaderId, String scheduleId, String sessionId,
            String userDetail) throws Exception {
        String targetUrl = ScheduleCommonTools.joinUrlElems(leaderId.split(":"));
        ScheduleClient client = clientFactory.getFeignClientByNodeUrl(targetUrl);
        client.deleteSchdule(sessionId, userDetail, scheduleId);
    }

    @DeleteMapping("/schedules/{schedule_id}")
    public void deleteSchedule(@PathVariable("schedule_id") String scheduleId,
                               @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
                               @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws Exception {
        ScheduleFullEntity info = service.getSchedule(scheduleId);
        if (info.getType().equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                    "schedule is not exist:schedule_id=" + scheduleId);
        }
        checkWsPriority(auth.getName(), info.getWorkspace(), ScmPrivilegeDefine.DELETE,
                "delete schedule");

        if (null != info.getTransitionId()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not deleted life cycle schedule");
        }

        if (!ScheduleElector.getInstance().isLeader()) {
            String leaderId = ScheduleElector.getInstance().getLeader();
            if (isLeaderStillMe(leaderId, ScheduleElector.getInstance().getId())) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                        "I'm not leader yet!:id=" + leaderId);
            }
            deleteSchedule2Leader(leaderId, scheduleId, sessionId, userDetail);
            audit.info(ScmAuditType.DELETE_SCHEDULE, auth, info.getWorkspace(), 0,
                    "delete schedule to leader, scheduleId=" + scheduleId + ", leaderId="
                            + leaderId);
        }
        else {
            service.deleteSchedule(scheduleId, false);
            audit.info(ScmAuditType.DELETE_SCHEDULE, auth, info.getWorkspace(), 0,
                    "delete schedule, scheduleId=" + scheduleId);
        }
    }
    
    @RequestMapping(value = "/schedules", method = RequestMethod.HEAD)
    public ResponseEntity<String> countSchedule(
            @RequestParam(value = RestCommonDefine.RestParam.KEY_QUERY_FILTER, required = false) BSONObject condition,
            HttpServletResponse response, Authentication auth) throws Exception {
        String message = "count schedule";
        if (null != condition) {
            message += " by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.SCHEDULE_DQL, auth, null, 0, message);
        long count = service.countSchedule(condition);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
        return ResponseEntity.ok("");
    }

    @GetMapping("/schedules/{schedule_id}")
    public ScheduleClientEntity getSchedule(@PathVariable("schedule_id") String scheduleId,
            HttpServletRequest request, HttpServletResponse response, Authentication auth)
            throws Exception {
        ScheduleFullEntity info = service.getSchedule(scheduleId);
        ScheduleClientEntity result = new ScheduleClientEntity(info);
        if (StringUtils.hasText(info.getTransitionId())) {
            TransitionScheduleEntity transitionSchedule = lifeCycleScheduleDao
                    .queryOne(new BasicBSONObject(
                            FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID,
                            info.getTransitionId()));
            result.setTransitionName(transitionSchedule.getTransition().getName());
        }
        if (info.getType().equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                    "schedule is not exist:schedule_id=" + scheduleId);
        }
        checkWsPriority(auth.getName(), info.getWorkspace(), ScmPrivilegeDefine.READ,
                "get schedule");
        audit.info(ScmAuditType.SCHEDULE_DQL, auth, info.getWorkspace(), 0,
                "get schedule by scheduleId=" + scheduleId);

        return result;
    }

    @GetMapping("/schedules")
    public void listSchedules(
            @RequestParam(value = RestCommonDefine.RestParam.KEY_QUERY_FILTER, required = false) String filter,
            @RequestParam(value = RestCommonDefine.RestParam.KEY_QUERY_ORDERBY, required = false) String orderBy,
            @RequestParam(value = RestCommonDefine.RestParam.KEY_QUERY_SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestCommonDefine.RestParam.KEY_QUERY_LIMIT, required = false, defaultValue = "-1") long limit,
            HttpServletResponse response,
            Authentication auth) throws Exception {
        BSONObject userCondition = null;
        BSONObject orderByCondition = null;
        if (null != filter) {
            userCondition = (BSONObject) JSON.parse(filter);
        }
        else {
            userCondition = new BasicBSONObject();
        }
        if (null != orderBy) {
            orderByCondition = (BSONObject) JSON.parse(orderBy);
        }

        BasicBSONList andArr = new BasicBSONList();
        andArr.add(userCondition);
        andArr.add(new BasicBSONObject(FieldName.Schedule.FIELD_TYPE,
                new BasicBSONObject("$ne", ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)));
        BSONObject condition = new BasicBSONObject("$and", andArr);

        ScmBSONObjectCursor cursor = null;
        try {
            cursor = service.listSchedule(condition, orderByCondition, skip, limit);

            audit.info(ScmAuditType.SCHEDULE_DQL, auth, null, 0,
                    "get schedule list,userCondition=" + userCondition.toString());

            response.setHeader(RestCommonDefine.CONTENT_TYPE,
                    RestCommonDefine.APPLICATION_JSON_UTF8);
            PrintWriter writer = response.getWriter();

            writer.write("[");
            boolean isFirstObj = true;
            while (cursor.hasNext()) {
                BSONObject obj = cursor.next();
                ScheduleFullEntity info = ScheduleEntityTranslator.FullInfo.fromBSONObject(obj);
                if (StringUtils.hasText(info.getTransitionId())) {
                    TransitionScheduleEntity transitionSchedule = lifeCycleScheduleDao
                            .queryOne(new BasicBSONObject(
                                    FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID,
                                    info.getTransitionId()));
                    obj.put(RestCommonField.REST_TRANSITION_NAME,
                            transitionSchedule.getTransition().getName());
                }
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

    @RequestMapping(value = "/schedules/{schedule_id}", method = RequestMethod.PUT)
    public ScheduleFullEntity updateScheduleInfo(
            @RequestAttribute(SecurityRestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(SecurityRestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("schedule_id") String scheduleId,
            @RequestParam(RestCommonDefine.RestParam.KEY_DESCRIPTION) String newInfoDesc,
            Authentication auth) throws Exception {

        ScheduleFullEntity info = service.getSchedule(scheduleId);

        if (info.getType().equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                    "schedule is not exist:schedule_id=" + scheduleId);
        }
        checkWsPriority(auth.getName(), info.getWorkspace(), ScmPrivilegeDefine.UPDATE,
                "update schedule");
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
            ScheduleFullEntity updateSchedule2Leader = updateSchedule2Leader(leaderId, scheduleId,
                    newInfoDesc, sessionId, userDetail);

            audit.info(ScmAuditType.UPDATE_SCHEDULE, auth, info.getWorkspace(), 0,
                    "update schedule to leader, scheduleId=" + scheduleId + ", leaderId="
                            + leaderId);

            return updateSchedule2Leader;
        }
        else {
            // SEQUOIACM-1084: 此处需要适配驱动的多余编码动作
            newInfoDesc = URLDecoder.decode(newInfoDesc, RestCommonDefine.CHARSET_UTF8);
            ScheduleNewUserInfo newInfo = ScheduleEntityTranslator.NewUserInfo
                    .fromJSON(newInfoDesc);
            ScheduleFullEntity updateSchedule = service.updateSchedule(scheduleId, newInfo);

            audit.info(ScmAuditType.UPDATE_SCHEDULE, auth, info.getWorkspace(), 0,
                    "update schedule, scheduleId=" + scheduleId);
            return updateSchedule;
        }
    }

    private ScheduleFullEntity updateSchedule2Leader(String leaderId, String scheduleId,
            String newInfoDesc, String sessionId, String userDetail) throws Exception {
        String leaderUrl = ScheduleCommonTools.joinUrlElems(leaderId.split(":"));
        ScheduleClient client = clientFactory.getFeignClientByNodeUrl(leaderUrl);
        return client.updateSchdule(sessionId, userDetail, scheduleId, newInfoDesc);
    }

    private boolean isLeaderStillMe(String leaderId, String myId) {
        if (leaderId.equals(myId)) {
            return true;
        }

        return false;
    }
}
