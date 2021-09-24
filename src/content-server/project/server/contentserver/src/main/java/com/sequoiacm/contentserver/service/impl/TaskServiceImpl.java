package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.job.ScmTaskBase;
import com.sequoiacm.contentserver.job.ScmTaskManager;
import com.sequoiacm.contentserver.job.TaskRemoveUpdator;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.service.ITaskService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.site.ScmContentServerInfo;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaTaskAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

@Service
public class TaskServiceImpl implements ITaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    // TODO:for a seemingly useless api (get /task/<id>)
    @Override
    public BSONObject getTask(String taskId) throws ScmServerException {
        return getTaskDetail(taskId);
    }

    @Override
    public BSONObject getTaskDetail(String taskId) throws ScmServerException {
        BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
        BSONObject taskInfo = ScmContentServer.getInstance().getTaskInfo(matcher);
        if (taskInfo == null) {
            throw new ScmServerException(ScmError.TASK_NOT_EXIST,
                    "task not exist:taskId=" + taskId);
        }

        return taskInfo;
    }

    @Override
    public MetaCursor getTaskList(BSONObject condition, BSONObject orderby, BSONObject selector,
            long skip, long limit) throws ScmServerException {
        MetaTaskAccessor accessor = ScmContentServer.getInstance().getMetaService().getMetaSource()
                .getTaskAccessor();

        try {
            if (null == selector) {
                selector = new BasicBSONObject();
                selector.put(FieldName.Task.FIELD_TYPE, null);
                selector.put(FieldName.Task.FIELD_ID, null);
                selector.put(FieldName.Task.FIELD_SCHEDULE_ID, null);
                selector.put(FieldName.Task.FIELD_WORKSPACE, null);
                selector.put(FieldName.Task.FIELD_TARGET_SITE, null);
                selector.put(FieldName.Task.FIELD_START_TIME, null);
                selector.put(FieldName.Task.FIELD_RUNNING_FLAG, null);
            }
            return accessor.query(condition, selector, orderby, skip, limit);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get task list, condition=" + condition
                    + ", orderby=" + orderby
                    + ", selector=" + selector
                    + ", skip=" + skip
                    + ", limit="+ limit, e);
        }
    }

    @Override
    public String startTask(String serssionId, String userDetail, String wsName, int taskType,
            int serverId, String targetSite, BSONObject options) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentServer.getWorkspaceInfoChecked(wsName);
        String taskId = "";

        ScmContentServerInfo serverInfo = contentServer.getServerInfo(serverId);
        if (null == serverInfo) {
            throw new ScmServerException(ScmError.SERVER_NOT_EXIST,
                    "server is not exist:serverId=" + serverId);
        }

        int sourceSiteId = serverInfo.getSite().getId();
        Integer targetSiteId = null;
        if (taskType == CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE) {
            // get remote site id
            Set<Integer> siteIds = wsInfo.getDataSiteIds();
            for (Integer id : siteIds) {
                ScmSite scmSite = contentServer.getSiteInfo(id);
                if (scmSite.getName().equals(targetSite)) {
                    targetSiteId = scmSite.getId();
                }
            }
            if (null == targetSiteId) {
                throw new ScmServerException(ScmError.SITE_NOT_EXIST,
                        "target site not exist in workspace:workspace=" + wsName + ",targetSite="
                                + targetSite);
            }
            // check
            ScmStrategyMgr.getInstance().checkTransferSite(wsInfo, sourceSiteId, targetSiteId);
        }
        else if (taskType == CommonDefine.TaskType.SCM_TASK_CLEAN_FILE) {
            ScmStrategyMgr.getInstance().checkCleanSite(wsInfo, sourceSiteId);
        }

        if (contentServer.isInMainSite()) {
            taskId = initAndNotifyTask(wsInfo, taskType, options, serverId, targetSiteId);
        }
        else {
            // transfer to the main site
            taskId = forwardStartToMainSite(serssionId, userDetail, wsName, taskType, options,
                    serverId, targetSite);
        }
        return taskId;
    }

    private String initAndNotifyTask(ScmWorkspaceInfo wsInfo, int taskType, BSONObject options,
            int serverId, Integer targetSiteId) throws ScmServerException {
        if (taskType != CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE
                && taskType != CommonDefine.TaskType.SCM_TASK_CLEAN_FILE) {
            throw new ScmInvalidArgumentException("unreconigzed task type:type=" + taskType);
        }

        // write task & notify
        BSONObject taskContent = (BSONObject) options.get(CommonDefine.RestArg.TASK_FILTER);
        if (taskContent == null) {
            taskContent = new BasicBSONObject();
        }
        int taskScope = checkAndGetScope(options);
        long maxExecTime = BsonUtils
                .getNumberOrElse(options, CommonDefine.RestArg.TASK_MAX_EXEC_TIME, 0).longValue();

        String taskId = ScmIdGenerator.TaskId.get();
        createTask(taskType, taskId, wsInfo.getName(), taskContent, serverId, targetSiteId,
                taskScope, maxExecTime);
        try {
            notifyTask(serverId, taskId, CommonDefine.TaskNotifyType.SCM_TASK_CREATE);
            logger.info("start task success:taskId=" + taskId);
        }
        catch (ScmServerException e) {
            removeTaskAndAsyncRedo(taskId);
            throw e;
        }
        catch (Exception e) {
            removeTaskAndAsyncRedo(taskId);
            throw new ScmSystemException("", e);
        }

        return taskId;
    }

    private void notifyTask(int serverId, String taskId, int notifyType) throws ScmServerException {

        ScmContentServerInfo serverInfo = ScmContentServer.getInstance().getServerInfo(serverId);
        if (null == serverInfo) {
            throw new ScmServerException(ScmError.SERVER_NOT_EXIST,
                    "server is unexist:serverId=" + serverId);
        }

        String hostPort = serverInfo.getHostName() + ":" + serverInfo.getPort();

        ContentServerClient client = ContentServerClientFactory.getFeignClientByNodeUrl(hostPort);
        client.notifyTask(taskId, notifyType);
    }

    private void removeTaskAndAsyncRedo(String taskId) {
        TaskRemoveUpdator removeUpdator = new TaskRemoveUpdator(taskId);
        try {
            removeUpdator.doUpdate();
        }
        catch (Exception e) {
            ScmTaskManager.getInstance().addAsyncTaskUpdator(removeUpdator);
            logger.warn("remove task failed:taskId=" + taskId);
        }
    }

    private BSONObject getNotFinishTaskMatcher(int taskType, String workspaceName)
            throws ScmServerException {
        BSONObject flagList = new BasicBSONList();
        flagList.put("0", CommonDefine.TaskRunningFlag.SCM_TASK_INIT);
        flagList.put("1", CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING);
        BSONObject inFlag = new BasicBSONObject(ScmMetaSourceHelper.SEQUOIADB_MATCHER_IN, flagList);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Task.FIELD_WORKSPACE, workspaceName);
        matcher.put(FieldName.Task.FIELD_RUNNING_FLAG, inFlag);

        return matcher;
    }

    private void createTask(int type, String id, String workspaceName, BSONObject content,
            int serverId, Integer targetSiteId, int taskScope, long maxExecTime)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        long time = new Date().getTime();

        BSONObject task = new BasicBSONObject();
        task.put(FieldName.Task.FIELD_ID, id);
        task.put(FieldName.Task.FIELD_TYPE, type);
        task.put(FieldName.Task.FIELD_WORKSPACE, workspaceName);
        task.put(FieldName.Task.FIELD_SCOPE, taskScope);
        task.put(FieldName.Task.FIELD_CONTENT, content);
        task.put(FieldName.Task.FIELD_SERVER_ID, serverId);
        task.put(FieldName.Task.FIELD_PROGRESS, 0);
        task.put(FieldName.Task.FIELD_RUNNING_FLAG, CommonDefine.TaskRunningFlag.SCM_TASK_INIT);
        task.put(FieldName.Task.FIELD_START_TIME, time);
        task.put(FieldName.Task.FIELD_STOP_TIME, null);
        task.put(FieldName.Task.FIELD_ESTIMATE_COUNT, 0L);
        task.put(FieldName.Task.FIELD_ACTUAL_COUNT, 0L);
        task.put(FieldName.Task.FIELD_SUCCESS_COUNT, 0L);
        task.put(FieldName.Task.FIELD_FAIL_COUNT, 0L);
        if (type == CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE) {
            task.put(FieldName.Task.FIELD_TARGET_SITE, targetSiteId);
        }
        task.put(FieldName.Task.FIELD_MAX_EXEC_TIME, maxExecTime);
        contentServer.insertTask(task);
    }

    private void forwardStopToMainSite(String sessionId, String userDetail, String taskId)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        int remoteSiteId = contentServer.getMainSite();
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByServiceName(contentServer.getSiteInfo(remoteSiteId).getName());
        client.stopTask(sessionId, userDetail, taskId);

    }

    private String forwardStartToMainSite(String sessionId, String userDetail, String wsName,
            int taskType, BSONObject options, int serverId, String targetSite)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        int remoteSiteId = contentServer.getMainSite();
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByServiceName(contentServer.getSiteInfo(remoteSiteId).getName());
        BSONObject res = client.startTask(sessionId, userDetail, wsName, taskType, serverId,
                targetSite, options.toString());
        BSONObject task = (BSONObject) res.get(CommonDefine.RestArg.TASK_INFO_RESP);
        String id = (String) task.get(CommonDefine.RestArg.TASK_ID);
        if (id == null) {
            // impossible
            throw new ScmSystemException("task id is null:sessionId=" + sessionId + ",wsName="
                    + wsName + ",taskType=" + taskType);
        }
        return id;
    }

    @Override
    public void stopTask(String sessionId, String userDetail, String taskId)
            throws ScmServerException {
        if (ScmContentServer.getInstance().isInMainSite()) {
            stopAndNotifyTask(taskId);
        }
        else {
            // transfer to the main site
            forwardStopToMainSite(sessionId, userDetail, taskId);
        }
    }

    @Override
    public void notifyTask(String taskId, int notifyType) throws ScmServerException {
        if (notifyType == CommonDefine.TaskNotifyType.SCM_TASK_CREATE) {
            startTask(taskId);
        }
        else if (notifyType == CommonDefine.TaskNotifyType.SCM_TASK_STOP) {
            stopTask(taskId);
        }
        else {
            throw new ScmInvalidArgumentException("unrecognized notify type:type=" + notifyType);
        }
    }

    @Override
    public long countTask(BSONObject condition) throws ScmServerException {
        MetaTaskAccessor accessor = ScmContentServer.getInstance().getMetaService().getMetaSource()
                .getTaskAccessor();
        try {
            return accessor.count(condition);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get task count, condition=" + condition, e);
        }
    }

    private void stopAndNotifyTask(String taskId) throws ScmServerException {
        BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
        BSONObject taskInfo = ScmContentServer.getInstance().getTaskInfo(matcher);
        if (null == taskInfo) {
            throw new ScmServerException(ScmError.TASK_NOT_EXIST,
                    "task is inexistent:taskId=" + taskId);
        }

        int runningFlag = (int) taskInfo.get(FieldName.Task.FIELD_RUNNING_FLAG);
        if (runningFlag != CommonDefine.TaskRunningFlag.SCM_TASK_INIT
                && runningFlag != CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING
                && runningFlag != CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL) {
            // have been finished
            logger.warn("ignore stop task:taskId=" + taskId + ",runningFlag=" + runningFlag);
            return;
        }

        ScmTaskBase.startCancelTask(taskId, "user canceled");

        int serverId = (int) taskInfo.get(FieldName.Task.FIELD_SERVER_ID);
        notifyTask(serverId, taskId, CommonDefine.TaskNotifyType.SCM_TASK_STOP);
        logger.info("stop task:taskId=" + taskId);
    }

    private void stopTask(String taskId) {
        ScmTaskBase task = null;
        try {
            task = ScmTaskManager.getInstance().getTask(taskId);
            if (null == task) {
                logger.warn("task have been stopped:taskId=" + taskId);
                return;
            }

            task.stop();
            logger.info("task stop by user:taskId=" + taskId);
        }
        catch (Exception e) {
            logger.warn("stop task failed", e);
        }
    }

    private void startTask(String taskId) throws ScmServerException {
        ScmTaskBase task = null;
        try {
            // 1. get task info
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject taskInfo = ScmContentServer.getInstance().getTaskInfo(matcher);
            if (null == taskInfo) {
                throw new ScmServerException(ScmError.TASK_NOT_EXIST,
                        "task is not exist:taskId=" + taskId);
            }

            int runningFlag = (int) taskInfo.get(FieldName.Task.FIELD_RUNNING_FLAG);
            if (runningFlag != CommonDefine.TaskRunningFlag.SCM_TASK_INIT) {
                // have been started
                return;
            }

            task = ScmTaskManager.getInstance().getTask(taskId);
            if (null != task) {
                // task is exist
                return;
            }

            // 2. create task instance
            task = ScmTaskManager.getInstance().createTask(taskInfo);

            // change task status in table
            task.startTask();
        }
        catch (ScmServerException e) {
            throw new ScmServerException(e.getError(),
                    "create task failed:taskId=" + taskId + ",error=" + e, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("create task failed:taskId=" + taskId + ",error=" + e, e);
        }

        // 4. start task background
        try {
            task.start();
            logger.info("task started:taskId=" + taskId);
        }
        catch (Exception e) {
            logger.error("start task failed:taskId=" + taskId, e);
        }
    }

    private int checkAndGetScope(BSONObject options) throws ScmServerException {
        Integer taskScope = (Integer) options.get(CommonDefine.RestArg.TASK_SCOPE);
        if (taskScope == null) {
            taskScope = CommonDefine.Scope.SCOPE_CURRENT;
        }
        else {
            if (taskScope != CommonDefine.Scope.SCOPE_CURRENT
                    && taskScope != CommonDefine.Scope.SCOPE_HISTORY
                    && taskScope != CommonDefine.Scope.SCOPE_ALL) {
                throw new ScmInvalidArgumentException(
                        "create task failed,unknow scope:scope=" + taskScope);
            }
            if (taskScope != CommonDefine.Scope.SCOPE_CURRENT) {
                // query history table, check the matcher
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(
                            (BSONObject) options.get(CommonDefine.RestArg.TASK_FILTER));
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("Invalid task filter", e);
                }
            }
        }
        return taskScope;
    }
}
