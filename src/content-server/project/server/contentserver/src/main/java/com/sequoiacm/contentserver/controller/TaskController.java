package com.sequoiacm.contentserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.ITaskService;
import com.sequoiacm.contentserver.service.IWorkspaceService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.metasource.MetaCursor;

@RestController
public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final ITaskService taskService;
    private final IWorkspaceService workspaceService;

    @Autowired
    public TaskController(ITaskService taskService, IWorkspaceService workspaceService) {
        this.taskService = taskService;
        this.workspaceService = workspaceService;
    }

    @GetMapping("/api/v1/tasks/{task_id}")
    public ResponseEntity task(@PathVariable("task_id") String taskId) throws ScmServerException {
        BSONObject task = taskService.getTask(taskId);
        Map<String, Object> result = new HashMap<>(1);
        result.put(CommonDefine.RestArg.TASK_INFO_RESP, task);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/api/v1/tasks/{task_id}", method = RequestMethod.HEAD)
    public ResponseEntity taskDetail(@PathVariable("task_id") String taskId,
            HttpServletResponse response) throws ScmServerException {
        BSONObject taskDetail = taskService.getTaskDetail(taskId);
        response.setHeader(CommonDefine.RestArg.TASK_INFO_RESP, taskDetail.toString());
        return ResponseEntity.ok("");
    }

    @GetMapping("/api/v1/tasks")
    public void taskList(@RequestParam(value = "filter", required = false) BSONObject filter,
            HttpServletResponse response) throws ScmServerException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = taskService.getTaskList(filter);
        ServiceUtils.putCursorToWriter(cursor, ServiceUtils.getWriter(response));
    }

    @RequestMapping(value = "/api/v1/tasks", method = { RequestMethod.POST, RequestMethod.PUT })
    @SuppressWarnings("unckecked")
    public ResponseEntity startTask(
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TYPE) int taskType,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_OPTIONS) String options,
            @RequestParam(value = CommonDefine.RestArg.CREATE_TASK_SERVER_ID, required = false) Integer serverId,
            @RequestParam(value = CommonDefine.RestArg.CREATE_TASK_TARGET_SITE, required = false) String targetSite,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String user,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        if (serverId == null) {
            logger.debug("lunch a task in local site:wsName={},type={}", workspaceName, taskType);
            serverId = ScmContentServer.getInstance().getId();
        }

        if (taskType == CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE && null == targetSite) {
            if (ScmStrategyMgr.getInstance().strategyType() == StrategyType.STAR) {
                targetSite = ScmContentServer.getInstance().getMainSiteName();
            }
            else {
                throw new ScmMissingArgumentException(
                        "Missing argument '" + CommonDefine.RestArg.CREATE_TASK_TARGET_SITE
                                + "',must be specified under the non-star strategy");
            }
        }

        BSONObject optionsBSON = (BSONObject) JSON.parse(options);

        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.UPDATE, "start task");

        String taskId = taskService.startTask(sessionId, user, workspaceName, taskType, serverId,
                targetSite, optionsBSON);

        Map<String, Object> result = new HashMap<>(1);
        Map<String, String> taskBasicInfo = new HashMap<>(1);
        taskBasicInfo.put(CommonDefine.RestArg.TASK_ID, taskId);
        result.put(CommonDefine.RestArg.TASK_INFO_RESP, taskBasicInfo);

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/api/v1/tasks/{task_id}/stop")
    public void stopTask(@PathVariable("task_id") String taskId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String user,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
        BSONObject taskInfo = ScmContentServer.getInstance().getTaskInfo(matcher);
        if (null == taskInfo) {
            throw new ScmServerException(ScmError.TASK_NOT_EXIST,
                    "task is inexistent:taskId=" + taskId);
        }

        String workspaceName = (String) taskInfo.get(FieldName.Task.FIELD_WORKSPACE);
        ScmFileServicePriv.getInstance().checkWsPriority(auth.getName(), workspaceName,
                ScmPrivilegeDefine.UPDATE, "stop task");
        taskService.stopTask(sessionId, user, taskId);
    }

    @PostMapping(value = "/internal/v1/tasks/{task_id}/notify")
    public void notifyTask(@PathVariable("task_id") String taskId,
            @RequestParam(CommonDefine.RestArg.TASK_NOTIFY_TYPE) int notifyType)
            throws ScmServerException {
        taskService.notifyTask(taskId, notifyType);
    }

}
