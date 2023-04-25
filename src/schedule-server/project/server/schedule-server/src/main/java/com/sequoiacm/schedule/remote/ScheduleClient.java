package com.sequoiacm.schedule.remote;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.schedule.common.RestCommonField;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import org.bson.BSONObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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

    @PostMapping(value = "/schedules")
    public ScheduleFullEntity createSchedule(
            @RequestParam(RestCommonDefine.RestParam.KEY_DESCRIPTION) String userEntityJson)
            throws ScheduleException;

    @DeleteMapping(value = "/schedules/{schedule_id}")
    public void deleteSchdule(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("schedule_id") String scheduleId) throws ScheduleException;

    @DeleteMapping(value = "/schedules/{schedule_id}")
    public void deleteSchdule(@PathVariable("schedule_id") String scheduleId,
            @RequestParam(RestCommonDefine.RestParam.STOP_WORKER) boolean stopWorker)
            throws ScheduleException;

    @PutMapping(value = "/schedules/{schedule_id}")
    public ScheduleFullEntity updateSchdule(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("schedule_id") String scheduleId,
            @RequestParam(RestCommonDefine.RestParam.KEY_DESCRIPTION) String description)
            throws ScheduleException;

    @PostMapping(value = "/tasks/{taskId}/notify")
    public void notifyTask(@PathVariable("taskId") String taskId,
            @RequestParam(RestCommonDefine.RestParam.KEY_NOTIFY_TYPE) int notifyType,
            @RequestParam(CommonDefine.RestArg.IS_ASYNC_COUNT_FILE) boolean isAsyncCountFile);

    @PostMapping(value = "/lifeCycleConfig")
    public void setLifeCycleConfig(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestParam(RestCommonField.LIFE_CYCLE_CONFIG) String userEntityJson)
            throws ScheduleException;

    @PostMapping(value = "/lifeCycleConfig/stageTag")
    public void addGlobalStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestParam(RestCommonField.STAGE_TAG_NAME) String stageTagName,
            @RequestParam(RestCommonField.STAGE_TAG_DESC) String stageTagDesc)
            throws ScheduleException;

    @DeleteMapping(value = "/lifeCycleConfig/stageTag/{stage_tag_name}")
    public void removeGlobalStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable(RestCommonField.REST_STAGE_TAG_NAME) String stageTagName)
            throws ScheduleException;

    @PostMapping("/lifeCycleConfig/transition")
    public void addGlobalTransition(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestParam(RestCommonField.TRANSITION) String transition) throws ScheduleException;

    @DeleteMapping("/lifeCycleConfig/transition/{transition_name}")
    public void removeGlobalTransition(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable(RestCommonField.REST_TRANSITION_NAME) String transitionName)
            throws ScheduleException;

    @PostMapping("/lifeCycleConfig/workspaces/{workspace_name}")
    public TransitionScheduleEntity wsApplyTransition(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable(RestCommonField.WORKSPACE_NAME) String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            @RequestParam(value = RestCommonField.TRANSITION, required = false) String transition,
            @RequestParam(RestCommonField.PREFERRED_REGION) String preferredRegion,
            @RequestParam(RestCommonField.PREFERRED_ZONE) String preferredZone)
            throws ScheduleException;

    @PutMapping("/lifeCycleConfig/workspaces/{workspace_name}")
    public TransitionScheduleEntity wsUpdateTransition(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable(RestCommonField.WORKSPACE_NAME) String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            @RequestParam(RestCommonField.TRANSITION) String transition,
            @RequestParam(RestCommonField.PREFERRED_REGION) String preferredRegion,
            @RequestParam(RestCommonField.PREFERRED_ZONE) String preferredZone)
            throws ScheduleException;

    @DeleteMapping("/lifeCycleConfig/workspaces/{workspace_name}")
    public void wsRemoveTransition(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable(RestCommonField.WORKSPACE_NAME) String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName)
            throws ScheduleException;

    @PutMapping("/lifeCycleConfig/transition/{transition_name}")
    public void updateGlobalTransition(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            @RequestParam(RestCommonField.TRANSITION) String transition) throws ScheduleException;

    @PutMapping("/lifeCycleConfig/workspaces/{workspace_name}?action=update_status")
    public void wsUpdateTransitionStatus(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable(RestCommonField.WORKSPACE_NAME) String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            @RequestParam(RestCommonField.TRANSITION_STATUS) Boolean status)
            throws ScheduleException;

    @PostMapping("/lifeCycleConfig/tasks/onceTransition")
    public BSONObject startOnceTransition(
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TYPE) int taskType,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_OPTIONS) String options,
            @RequestParam(RestCommonField.ONCE_TRANSITION_SOURCE) String sourceStageTag,
            @RequestParam(RestCommonField.ONCE_TRANSITION_DEST) String destStageTag,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestParam(RestCommonField.PREFERRED_REGION) String preferredRegion,
            @RequestParam(RestCommonField.PREFERRED_ZONE) String preferredZone,
            @RequestParam(CommonDefine.RestArg.IS_ASYNC_COUNT_FILE) boolean isAsyncCountFile)
            throws ScheduleException;

    @PostMapping(value = "/tasks")
    public BSONObject createMoveCopyTask(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestHeader(RestField.USER_ATTRIBUTE) String user,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TYPE) int taskType,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TARGET_SITE) String targetSite,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_OPTIONS) String options,
            @RequestParam(CommonDefine.RestArg.IS_ASYNC_COUNT_FILE) boolean isAsyncCountFile)
            throws ScheduleException;

    @DeleteMapping("/lifeCycleConfig")
    public void deleteLifeCycleConfig(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId) throws ScheduleException;

    @PostMapping("/lifeCycleConfig/sites/stageTag/{site_name}")
    public void setSiteStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("site_name") String siteName, @RequestParam("stage_tag") String stageTag)
            throws ScheduleException;

    @PutMapping("/lifeCycleConfig/sites/stageTag/{site_name}")
    public void alterSiteStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("site_name") String siteName, @RequestParam("stage_tag") String stageTag)
            throws ScheduleException;

    @DeleteMapping("/lifeCycleConfig/sites/stageTag/{site_name}")
    public void deleteSiteStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("site_name") String siteName) throws ScheduleException;
}
