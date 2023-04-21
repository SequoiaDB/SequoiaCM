package com.sequoiacm.contentserver.controller;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.ScmServer;
import com.sequoiacm.contentserver.exception.ScmMissingArgumentException;
import com.sequoiacm.contentserver.service.ITaskService;
import com.sequoiacm.contentserver.service.IWorkspaceService;
import com.sequoiacm.contentserver.service.impl.ServiceUtils;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.strategy.element.StrategyType;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

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
            @RequestParam(value = "orderby", required = false) BSONObject orderby,
            @RequestParam(value = "selector", required = false) BSONObject selector,
            @RequestParam(value = "skip", required = false, defaultValue = "0") long skip,
            @RequestParam(value = "limit", required = false, defaultValue = "-1") long limit,
            HttpServletResponse response) throws ScmServerException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        MetaCursor cursor = taskService.getTaskList(filter, orderby, selector, skip, limit);
        ServiceUtils.putCursorToResponse(cursor, response);
    }

    @RequestMapping(value = "/api/v1/tasks", method = { RequestMethod.POST, RequestMethod.PUT })
    @SuppressWarnings("unckecked")
    public ResponseEntity startTask(
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_TYPE) int taskType,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(CommonDefine.RestArg.CREATE_TASK_OPTIONS) String options,
            @RequestParam(value = CommonDefine.RestArg.CREATE_TASK_SERVER_ID, required = false) Integer serverId,
            @RequestParam(value = CommonDefine.RestArg.CREATE_TASK_TARGET_SITE, required = false) String targetSite,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        if (serverId == null) {
            logger.debug("lunch a task in local site:wsName={},type={}", workspaceName, taskType);
            serverId = ScmServer.getInstance().getContentServerInfo().getId();
        }

        if (taskType == CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE && null == targetSite) {
            if (ScmStrategyMgr.getInstance().strategyType() == StrategyType.STAR) {
                targetSite = ScmContentModule.getInstance().getMainSiteName();
            }
            else {
                throw new ScmMissingArgumentException(
                        "Missing argument '" + CommonDefine.RestArg.CREATE_TASK_TARGET_SITE
                                + "',must be specified under the non-star strategy");
            }
        }

        if (taskType == CommonDefine.TaskType.SCM_TASK_MOVE_FILE && null == targetSite) {
            throw new ScmMissingArgumentException(
                    "Missing argument " + CommonDefine.RestArg.CREATE_TASK_TARGET_SITE);
        }

        BSONObject optionsBSON = (BSONObject) JSON.parse(options);

        ScmUser user = (ScmUser) auth.getPrincipal();

        String taskId = taskService.startTask(sessionId, userDetail, user, workspaceName, taskType,
                serverId, targetSite, optionsBSON);

        Map<String, Object> result = new HashMap<>(1);
        Map<String, String> taskBasicInfo = new HashMap<>(1);
        taskBasicInfo.put(CommonDefine.RestArg.TASK_ID, taskId);
        result.put(CommonDefine.RestArg.TASK_INFO_RESP, taskBasicInfo);

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/api/v1/tasks/{task_id}/stop")
    public void stopTask(@PathVariable("task_id") String taskId,
            @RequestAttribute(RestField.USER_ATTRIBUTE) String userDetail,
            @RequestHeader(RestField.SESSION_ATTRIBUTE) String sessionId, Authentication auth)
            throws ScmServerException {
        BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
        BSONObject taskInfo = ScmContentModule.getInstance().getTaskInfo(matcher);
        if (null == taskInfo) {
            throw new ScmServerException(ScmError.TASK_NOT_EXIST,
                    "task is inexistent:taskId=" + taskId);
        }

        ScmUser user = (ScmUser) auth.getPrincipal();
        String workspaceName = (String) taskInfo.get(FieldName.Task.FIELD_WORKSPACE);
        taskService.stopTask(sessionId, userDetail, user, workspaceName, taskId);
    }

    @PostMapping(value = "/internal/v1/tasks/{task_id}/notify")
    public void notifyTask(@PathVariable("task_id") String taskId,
            @RequestParam(CommonDefine.RestArg.TASK_NOTIFY_TYPE) int notifyType)
            throws ScmServerException {
        taskService.notifyTask(taskId, notifyType);
    }

    @RequestMapping(value = "/api/v1/tasks", method = RequestMethod.HEAD)
    public void countTask(
            @RequestParam(value = CommonDefine.RestArg.TASK_FILTER, required = false) BSONObject condition,
            HttpServletResponse response) throws ScmServerException {
        long count = taskService.countTask(condition);
        response.setHeader(CommonDefine.RestArg.X_SCM_COUNT, String.valueOf(count));
    }

}
