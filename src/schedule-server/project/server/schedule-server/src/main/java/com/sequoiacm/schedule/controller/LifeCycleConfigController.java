package com.sequoiacm.schedule.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmWorkspaceResource;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.RestCommonField;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.common.model.LifeCycleConfigFullEntity;
import com.sequoiacm.schedule.common.model.LifeCycleConfigUserEntity;
import com.sequoiacm.schedule.common.model.LifeCycleEntityTranslator;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.TransitionEntityTranslator;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.common.model.TransitionUserEntity;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.elect.ScheduleElector;
import com.sequoiacm.schedule.privilege.ScmSchedulePriv;
import com.sequoiacm.schedule.remote.ScheduleClient;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import com.sequoiacm.schedule.service.LifeCycleConfigService;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LifeCycleConfigController {

    private static final Logger logger = LoggerFactory.getLogger(LifeCycleConfigController.class);

    @Autowired
    private LifeCycleConfigService service;

    @Autowired
    private ScheduleClientFactory clientFactory;

    @PostMapping("/lifeCycleConfig")
    public void setLifeCycleConfig(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestParam(RestCommonField.LIFE_CYCLE_CONFIG) String config, Authentication auth)
            throws ScheduleException {
        LifeCycleConfigUserEntity info = LifeCycleEntityTranslator.UserInfo.analyzeConfig(config);
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "set global life cycle config");
        if (!ScheduleElector.getInstance().isLeader()) {
            // createGlobalConfig2Leader
            ScheduleClient client = getScheduleClient();
            client.setLifeCycleConfig(sessionId, userDetail,
                    LifeCycleEntityTranslator.UserInfo.toJSONString(info));
        }
        else {
            service.setGlobalLifeCycleConfig(user.getUsername(), info);
        }
    }

    @DeleteMapping("/lifeCycleConfig")
    public void deleteLifeCycleConfig(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws Exception {
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "delete global life cycle config");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.deleteLifeCycleConfig(userDetail, sessionId);
        }
        else {
            service.deleteLifeCycleConfig();
        }
    }

    @GetMapping("/lifeCycleConfig")
    public LifeCycleConfigFullEntity getLifeCycleConfig() throws ScheduleException {
        BSONObject globalLifeCycleConfig = service.getGlobalLifeCycleConfig();
        if (globalLifeCycleConfig == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "life cycle config not found.");
        }
        return LifeCycleEntityTranslator.FullInfo.fromBSONObject(globalLifeCycleConfig);
    }

    @PostMapping("/lifeCycleConfig/stageTag")
    public void addGlobalStageTag(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestParam(RestCommonField.STAGE_TAG_NAME) String stageTagName,
            @RequestParam(value = RestCommonField.STAGE_TAG_DESC, required = false) String stageTagDesc,
            Authentication auth)
            throws Exception {
        if (!StringUtils.hasText(stageTagName)) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "stage tag name can not be null,stageTagName=" + stageTagName);
        }
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "add global stage tag");
        if (!ScheduleElector.getInstance().isLeader()) {
            // addGlobalStageTag2Leader
            ScheduleClient client = getScheduleClient();
            client.addGlobalStageTag(sessionId, userDetail, stageTagName, stageTagDesc);
        }
        else {
            service.addGlobalStageTag(user.getUsername(), stageTagName, stageTagDesc);
        }
    }

    @DeleteMapping("/lifeCycleConfig/stageTag/{stage_tag_name}")
    public void removeGlobalStageTag(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("stage_tag_name") String stageTagName,
            Authentication auth)
            throws Exception {
        if (!StringUtils.hasText(stageTagName)) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "stage tag name can not be null,stageTagName=" + stageTagName);
        }
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "remove global stage tag");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.removeGlobalStageTag(sessionId, userDetail, stageTagName);
        }
        else {
            service.removeGlobalStageTag(stageTagName, user.getUsername());
        }
    }

    @GetMapping("/lifeCycleConfig/stageTag")
    public BSONObject listGlobalStageTag() throws ScheduleException {
        return service.listGlobalStageTag();
    }

    @PostMapping("/lifeCycleConfig/transition")
    public void addGlobalTransition(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestParam(RestCommonField.TRANSITION) String transition, Authentication auth)
            throws Exception {
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "add global transition");
        TransitionUserEntity info = TransitionEntityTranslator.UserInfo.analyzeConfig(transition);
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.addGlobalTransition(sessionId, userDetail,
                    TransitionEntityTranslator.UserInfo.toJSONString(info));
        }
        else {
            service.addGlobalTransition(info, user.getUsername());
        }
    }

    @PutMapping("/lifeCycleConfig/transition/{transition_name}")
    public void updateGlobalTransition(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("transition_name") String transitionName,
            @RequestParam(RestCommonField.TRANSITION) String transition, Authentication auth)
            throws Exception {
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "update global transition");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.updateGlobalTransition(userDetail, sessionId, transitionName, transition);
        }
        else {
            TransitionUserEntity info = TransitionEntityTranslator.UserInfo
                    .analyzeConfig(transition);
            service.updateGlobalTransition(transitionName, info, user.getUsername());
        }
    }

    @DeleteMapping("/lifeCycleConfig/transition/{transition_name}")
    public void removeGlobalTransition(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("transition_name") String transitionName,
            Authentication auth)
            throws Exception {
        if (!StringUtils.hasText(transitionName)) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "" + "transition name can not be null,transitionName=" + transitionName);
        }
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "remove global transition");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.removeGlobalTransition(sessionId, userDetail, transitionName);
        }
        else {
            service.removeGlobalTransition(transitionName, user.getUsername());
        }
    }

    @GetMapping(path = "/lifeCycleConfig/transition/{transition_name}", params = "action=find_transition")
    public TransitionUserEntity getGlobalTransition(
            @PathVariable("transition_name") String transitionName,
            Authentication auth)
            throws Exception {
        if (!StringUtils.hasText(transitionName)) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "" + "transition name can not be null,transitionName=" + transitionName);
        }
        return service.getGlobalTransitionByName(transitionName);
    }

    @PostMapping("/lifeCycleConfig/workspaces/{workspace_name}")
    public TransitionScheduleEntity wsApplyTransition(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("workspace_name") String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            @RequestParam(value = RestCommonField.TRANSITION, required = false) String transition,
            @RequestParam(RestCommonField.PREFERRED_REGION) String preferredRegion,
            @RequestParam(RestCommonField.PREFERRED_ZONE) String preferredZone, Authentication auth)
            throws Exception {
        checkWsPriority(auth.getName(), workspace, ScmPrivilegeDefine.CREATE,
                "workspace apply transition");
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            return client.wsApplyTransition(userDetail, sessionId, workspace, transitionName,
                    transition, preferredRegion, preferredZone);
        }
        else {
            TransitionUserEntity info = null;
            if (StringUtils.hasText(transition)) {
                info = TransitionEntityTranslator.UserInfo.analyzeConfig(transition);
            }
            return service.wsApplyTransition(user.getUsername(), workspace, transitionName, info,
                    preferredRegion, preferredZone);
        }
    }

    @PutMapping("/lifeCycleConfig/workspaces/{workspace_name}")
    public TransitionScheduleEntity wsUpdateTransition(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("workspace_name") String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            @RequestParam(RestCommonField.TRANSITION) String transition,
            @RequestParam(value = RestCommonField.PREFERRED_REGION, required = false) String preferredRegion,
            @RequestParam(value = RestCommonField.PREFERRED_ZONE, required = false) String preferredZone,
            Authentication auth)
            throws Exception {
        checkWsPriority(auth.getName(), workspace, ScmPrivilegeDefine.UPDATE,
                "alter workspace transition");
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            return client.wsUpdateTransition(userDetail, sessionId, workspace, transitionName,
                    transition, preferredRegion, preferredZone);
        }
        else {
            TransitionUserEntity info = null;
            if (StringUtils.hasText(transition)) {
                info = TransitionEntityTranslator.UserInfo.analyzeConfig(transition);
            }
            return service.wsUpdateTransition(user.getUsername(), workspace, transitionName, info,
                    preferredRegion, preferredZone);
        }
    }

    @PutMapping(path = "/lifeCycleConfig/workspaces/{workspace_name}", params = "action=update_status")
    public void wsUpdateTransitionStatus(
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("workspace_name") String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            @RequestParam(RestCommonField.TRANSITION_STATUS) Boolean status, Authentication auth)
            throws Exception {
        checkWsPriority(auth.getName(), workspace, ScmPrivilegeDefine.UPDATE,
                "alter workspace transition status");
        ScmUser user = (ScmUser) auth.getPrincipal();
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.wsUpdateTransitionStatus(userDetail, sessionId, workspace, transitionName,
                    status);
        }
        else {
            service.wsUpdateTransitionStatus(user.getUsername(), workspace, transitionName, status);
        }
    }

    @DeleteMapping("/lifeCycleConfig/workspaces/{workspace_name}")
    public void wsRemoveTransition(@RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("workspace_name") String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            Authentication auth) throws Exception {
        checkWsPriority(auth.getName(), workspace, ScmPrivilegeDefine.DELETE,
                "workspace remove transition");
        if (!StringUtils.hasText(transitionName)) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "invalid argument transition name, transitionName=" + transitionName);
        }
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.wsRemoveTransition(userDetail, sessionId, workspace, transitionName);
        }
        else {
            service.wsRemoveTransition(workspace, transitionName);
        }
    }

    @GetMapping(path = "/lifeCycleConfig/workspaces/{workspace_name}", params = "action=find_transition")
    public TransitionScheduleEntity findWsTransition(
            @PathVariable("workspace_name") String workspace,
            @RequestParam(RestCommonField.REST_TRANSITION_NAME) String transitionName,
            Authentication auth) throws Exception {
        checkWsPriority(auth.getName(), workspace, ScmPrivilegeDefine.READ,
                "get workspace transition");
        if (!StringUtils.hasText(transitionName)) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "invalid argument transition name, transitionName=" + transitionName);
        }
        TransitionScheduleEntity info = service.findWsTransition(workspace, transitionName);
        if (info == null) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS,
                    "transition not exist, transitionName=" + transitionName + ", workspace"
                            + workspace);
        }
        return info;
    }

    @GetMapping(path = "/lifeCycleConfig/workspaces/{workspace_name}", params = "action=list_transition")
    public List<TransitionScheduleEntity> listWsTransition(
            @PathVariable("workspace_name") String workspace, Authentication auth)
            throws Exception {
        checkWsPriority(auth.getName(), workspace, ScmPrivilegeDefine.READ,
                "get workspace transition list");
        return service.listWsTransition(workspace);
    }

    @GetMapping(path = "/lifeCycleConfig/transition")
    public List<TransitionUserEntity> listGlobalTransition(
            @RequestParam(value = RestCommonField.REST_FLOW_STAGE_TAG_NAME, required = false) String stageTag)
            throws Exception {
        if (stageTag == null) {
            return service.listGlobalTransition();
        }
        return service.listGlobalTransitionByStageTag(stageTag);
    }

    @GetMapping(path = "/lifeCycleConfig/transition/{transition_name}", params = "action=list_workspace")
    public BSONObject listWsApplyTransition(
            @PathVariable("transition_name") String transitionName,
            Authentication auth) throws Exception {
        if (!StringUtils.hasText(transitionName)) {
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "invalid argument transition name, transitionName=" + transitionName);
        }
        return service.listWsApplyTransition(transitionName);
    }

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
            @RequestParam(RestCommonField.PREFERRED_ZONE) String preferredZone, Authentication auth)
            throws Exception {
        checkWsPriority(auth.getName(), workspaceName, ScmPrivilegeDefine.CREATE,
                "start once transition");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            return client.startOnceTransition(taskType, workspaceName, options, sourceStageTag,
                    destStageTag, userDetail, sessionId, preferredRegion, preferredZone);
        }
        else {
            return service.startOnceTransition(workspaceName, options, sourceStageTag, destStageTag,
                    userDetail, sessionId, preferredRegion, preferredZone, taskType);
        }

    }

    @PostMapping("/lifeCycleConfig/sites/stageTag/{site_name}")
    public void setSiteStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("site_name") String siteName, @RequestParam("stage_tag") String stageTag,
            Authentication auth) throws Exception {
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "set site stage tag");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.setSiteStageTag(sessionId, userDetail, siteName, stageTag);
        }
        else {
            service.setSiteStageTag(siteName, stageTag);
        }
    }

    @PutMapping("/lifeCycleConfig/sites/stageTag/{site_name}")
    public void alterSiteStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("site_name") String siteName, @RequestParam("stage_tag") String stageTag,
            Authentication auth) throws Exception {
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "alter site stage tag");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.alterSiteStageTag(sessionId, userDetail, siteName, stageTag);
        }
        else {
            service.alterSiteStageTag(siteName, stageTag);
        }
    }

    @DeleteMapping("/lifeCycleConfig/sites/stageTag/{site_name}")
    public void deleteSiteStageTag(@RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @PathVariable("site_name") String siteName, Authentication auth) throws Exception {
        ScmUser user = (ScmUser) auth.getPrincipal();
        checkAdminPriority(user, "alter site stage tag");
        if (!ScheduleElector.getInstance().isLeader()) {
            ScheduleClient client = getScheduleClient();
            client.deleteSiteStageTag(sessionId, userDetail, siteName);
        }
        else {
            service.deleteSiteStageTag(siteName);
        }
    }

    @GetMapping("/lifeCycleConfig/sites/stageTag/{site_name}")
    public ResponseEntity getSiteStageTag(
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId,
            @PathVariable("site_name") String siteName, Authentication auth)
            throws ScheduleException {
        String siteStageTag = service.getSiteStageTag(siteName);
        Map<String, String> result = new HashMap<>(1);
        result.put(FieldName.FIELD_CLSITE_STAGE_TAG, siteStageTag);
        return ResponseEntity.ok(result);
    }

    void checkWsPriority(String userName, String wsName, ScmPrivilegeDefine op, String opDesc)
            throws ScheduleException {
        if (ScheduleServer.getInstance().getWorkspace(wsName) == null) {
            logger.error("workspace is not exist:ws={}", wsName);
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.WORKSPACE_NOT_EXISTS,
                    opDesc + " failed, workspace is not exist:ws=" + wsName);
        }
        if (!ScmSchedulePriv.getInstance().hasPriority(userName, ScmWorkspaceResource.RESOURCE_TYPE,
                wsName, op)) {
            logger.error("do not have priority to {}:user={},ws={}", opDesc, userName, wsName);
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                    opDesc + " failed, do not have priority:user=" + userName + ",ws=" + wsName);
        }
    }

    private void checkAdminPriority(ScmUser user, String opDesc) throws ScheduleException {
        if (!user.hasRole(ScmRole.AUTH_ADMIN_ROLE_NAME)) {
            logger.error("do not have priority to {}:user={}", opDesc, user.getUsername());
            throw new ScheduleException(
                    com.sequoiacm.schedule.common.RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                    opDesc + " failed, do not have priority:user=" + user.getUsername());
        }
    }

    private ScheduleClient getScheduleClient() throws ScheduleException {
        String leaderId = getScheduleLeaderId();
        String leaderUrl = null;
        try {
            leaderUrl = ScheduleCommonTools.joinUrlElems(leaderId.split(":"));
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "failed to get leaderUrl", e);
        }
        return clientFactory.getFeignClientByNodeUrl(leaderUrl);
    }

    private String getScheduleLeaderId() throws ScheduleException {
        String leaderId = ScheduleElector.getInstance().getLeader();
        if (isLeaderStillMe(leaderId, ScheduleElector.getInstance().getId())) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                    "I'm not leader yet!:id=" + leaderId);
        }

        if ("".equals(leaderId)) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.PERMISSION_DENIED,
                    "there is not leader!:id=" + leaderId);
        }
        return leaderId;
    }

    private boolean isLeaderStillMe(String leaderId, String myId) {
        return leaderId.equals(myId);
    }
}
