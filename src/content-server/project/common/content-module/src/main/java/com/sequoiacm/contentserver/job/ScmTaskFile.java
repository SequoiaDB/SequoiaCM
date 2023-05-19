package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.FileCommonOperator;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.sequoiadb.dataopertion.SdbDataReaderImpl;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public abstract class ScmTaskFile extends ScmTaskBase {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskFile.class);

    protected BSONObject taskInfo;
    private String taskId;
    private BSONObject taskMatcher;
    private int mainSiteId;
    private int localSiteId;
    private ScmWorkspaceInfo wsInfo;

    private boolean running = true;
    private BSONObject actualMatcher;
    private int scope = CommonDefine.Scope.SCOPE_CURRENT;
    private long maxExecTime = 0; // 0 means unlimited

    private long taskStartTime;

    private boolean isQuickStart;
    private String dataCheckLevel = CommonDefine.DataCheckLevel.WEEK;

    private boolean isAsyncCountFile;

    // private int realTimeProgress = 0;

    public ScmTaskFile(ScmTaskManager mgr, BSONObject info, boolean isAsyncCountFile)
            throws ScmServerException {
        super(mgr);
        try {
            this.isAsyncCountFile = isAsyncCountFile;
            taskInfo = info;
            taskId = (String) info.get(FieldName.Task.FIELD_ID);
            taskMatcher = (BSONObject) info.get(FieldName.Task.FIELD_CONTENT);
            String wsName = (String) info.get(FieldName.Task.FIELD_WORKSPACE);
            if (info.containsField(FieldName.Task.FIELD_SCOPE)) {
                scope = (int) info.get(FieldName.Task.FIELD_SCOPE);
            }
            BSONObject option = BsonUtils.getBSON(info, FieldName.Task.FIELD_OPTION);
            if (option != null) {
                isQuickStart = BsonUtils.getBooleanOrElse(option,
                        FieldName.Task.FIELD_OPTION_QUICK_START, false);
                dataCheckLevel = BsonUtils.getStringOrElse(option,
                        FieldName.Task.FIELD_OPTION_DATA_CHECK_LEVEL,
                        CommonDefine.DataCheckLevel.WEEK);
            }
            ScmContentModule contentModule = ScmContentModule.getInstance();
            wsInfo = contentModule.getWorkspaceInfoCheckExist(wsName);
            mainSiteId = contentModule.getMainSite();
            localSiteId = contentModule.getLocalSite();

            actualMatcher = buildActualMatcher();
            maxExecTime = BsonUtils.getLongOrElse(info, FieldName.Task.FIELD_MAX_EXEC_TIME,
                    maxExecTime);
            // isQuickStart 决定是否统计文件数量，!isQuickStart：统计
            // isAsyncCountFile 决定是否同步统计文件数量。!isAsyncCountFile：同步
            if (!isQuickStart && !isAsyncCountFile) {
                countTaskFile();
            }
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("initializing ScmTaskTransferFile failed", e);
        }
    }

    public int getMainSiteId() {
        return mainSiteId;
    }

    public int getLocalSiteId() {
        return localSiteId;
    }

    public ScmWorkspaceInfo getWorkspaceInfo() {
        return wsInfo;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void _stop() {
        running = false;
    }

    public BSONObject getTaskContent() {
        return taskMatcher;
    }

    public boolean isQuickStart() {
        return isQuickStart;
    }

    public String getDataCheckLevel() {
        return dataCheckLevel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(getTaskId()).append(",type=").append(getTaskType())
                .append(",content=").append(getTaskContent().toString());

        return sb.toString();
    }

    protected void closeReader(SdbDataReaderImpl reader) {
        FileCommonOperator.closeReader(reader);
    }

    public static int getTaskRunningFlag(String taskId) throws ScmServerException {
        BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);

        BSONObject task = ScmContentModule.getInstance().getTaskInfo(matcher);
        if (null == task) {
            throw new ScmServerException(ScmError.TASK_NOT_EXIST,
                    "task is inexistent:taskId=" + taskId);
        }

        return (int) task.get(FieldName.Task.FIELD_RUNNING_FLAG);
    }

    protected MetaCursor getCursor(ScmMetaService sms) throws ScmServerException {
        switch (scope) {
            case CommonDefine.Scope.SCOPE_CURRENT:
                return sms.queryCurrentFile(wsInfo, actualMatcher, null,
                        createOderBy(CommonDefine.Scope.SCOPE_CURRENT), 0, -1, false);
            case CommonDefine.Scope.SCOPE_ALL:
                return sms.queryAllFile(wsInfo, actualMatcher, null,
                        createOderBy(CommonDefine.Scope.SCOPE_ALL));
            case CommonDefine.Scope.SCOPE_HISTORY:
                return sms.queryHistoryFile(wsInfo.getMetaLocation(), wsInfo.getName(),
                        actualMatcher, null, createOderBy(CommonDefine.Scope.SCOPE_HISTORY), 0, -1);
            default:
                throw new ScmInvalidArgumentException(
                        "runtask failed,unknow scope type:taskId=" + taskId + ",scope=" + scope);
        }
    }

    private BSONObject createOderBy(int scope) throws ScmServerException {
        // 检查文件表、历史表索引字段 data_create_time 是否存在，存在则按 data_create_time 升序排序，否则按
        // create_month 升序排序
        try {
            ContentModuleMetaSource metaSource = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource();
            if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
                if (metaSource.getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null)
                        .isIndexFieldExist(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME)) {
                    return new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, 1);
                }
            }
            else if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
                if (metaSource
                        .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null)
                        .isIndexFieldExist(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME)) {
                    return new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, 1);
                }
            }
            else if (scope == CommonDefine.Scope.SCOPE_ALL) {
                if (metaSource.getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(), null)
                        .isIndexFieldExist(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME)
                        && metaSource
                                .getFileHistoryAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                                        null)
                                .isIndexFieldExist(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME)) {
                    return new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, 1);
                }
            }
            else {
                throw new ScmInvalidArgumentException(
                        "run task failed,unknown scope type:taskId=" + taskId + ",scope=" + scope);
            }
        }
        catch (SdbMetasourceException e) {
            throw new ScmSystemException("failed to create orderBy condition, scope=" + scope, e);
        }
        return new BasicBSONObject(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, 1);

    }

    protected void closeCursor(MetaCursor cursor) {
        try {
            if (null != cursor) {
                cursor.close();
            }
        }
        catch (Exception e) {
            logger.warn("close task cursor failed:taskId=" + getTaskId(), e);
        }
    }

    private void countTaskFile() throws ScmServerException {
        long actualCount = 0;
        long estimateCount = 0;
        ScmMetaService sms = ScmContentModule.getInstance().getMetaService();
        try {
            switch (scope) {
                case CommonDefine.Scope.SCOPE_CURRENT:
                    actualCount = sms.getCurrentFileCount(wsInfo, actualMatcher);
                    estimateCount = sms.getCurrentFileCount(wsInfo, taskMatcher);
                    break;
                case CommonDefine.Scope.SCOPE_ALL:
                    actualCount = sms.getAllFileCount(wsInfo.getMetaLocation(), wsInfo.getName(),
                            actualMatcher);
                    estimateCount = sms.getAllFileCount(wsInfo.getMetaLocation(), wsInfo.getName(),
                            taskMatcher);
                    break;
                case CommonDefine.Scope.SCOPE_HISTORY:
                    actualCount = sms.getHistoryFileCount(wsInfo.getMetaLocation(),
                            wsInfo.getName(), actualMatcher);
                    estimateCount = sms.getHistoryFileCount(wsInfo.getMetaLocation(),
                            wsInfo.getName(), taskMatcher);
                    break;
                default:
                    throw new ScmInvalidArgumentException("runtask failed,unknow scope type:taskId="
                            + taskId + ",scope=" + scope);
            }
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.INVALID_ARGUMENT) {
                throw e;
            }
            logger.warn("count file failed:taskId={},wsName={}", taskId, wsInfo.getName(), e);
        }
        taskInfoContext.setEstimateCount(estimateCount);
        taskInfoContext.setActualCount(actualCount);
    }

    protected abstract void doFile(BSONObject fileInfoNotInLock) throws ScmServerException;

    protected abstract void taskComplete();

    protected abstract BSONObject buildActualMatcher() throws ScmServerException;

    @Override
    public final void _runTask() {
        // for interrupt task if exceed maxExecTime
        taskStartTime = getStartTime();
        // 任务可能在等待队列等待了很久，等待的时间可能超过了任务最大执行时间，这里需要先判断是否已经超时
        if (isTimeOut()) {
            logger.warn("task timeout:taskId={},startTime={},now={},maxExecTime={}ms", getTaskId(),
                    taskStartTime, new Date(), maxExecTime);
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_TIMEOUT,
                    "timeout", 0, 0, 0);
            return;
        }

        try {
            if (!isQuickStart && isAsyncCountFile) {
                countTaskFile();
                updateTaskFileCount(getTaskId(), taskInfoContext.getEstimateCount(),
                        taskInfoContext.getActualCount());
            }
        }
        catch (Exception e) {
            logger.error("failed to init task file count,taskId={}", getTaskId(), e);
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    e.toString(), 0, 0, 0);
            return;
        }

        long startExecuteTime = System.currentTimeMillis();
        logger.info("running task:task=" + toString());
        // 1. get total count
        // totalCount = queryMatchingCount();
        logger.info("task file info:taskId=" + getTaskId() + ",actual count="
                + taskInfoContext.getActualCount());

        ScmMetaService sms = null;
        MetaCursor cursor = null;
        try {
            sms = ScmContentModule.getInstance().getMetaService();
            updateTaskStartExecuteTime(taskId, startExecuteTime);
        }
        catch (ScmServerException e) {
            logger.warn("do task failed:taskId=" + getTaskId(), e);
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    e.toString(), 0, 0, 0);
            return;
        }

        try {
            cursor = getCursor(sms);

            // loop begin
            while (running && cursor.hasNext()) {
                BSONObject fileInfo = cursor.getNext();

                doFile(fileInfo);

                if (taskInfoContext.isAborted()) {
                    throw taskInfoContext.getAbortException();
                }
                int flag = getTaskRunningFlag(getTaskId());
                if (CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == flag) {
                    taskCancel();
                    return;
                }
                if (isTimeOut()) {
                    taskTimeout();
                    return;
                }
            }
            // loop end

            if (!running) {
                taskCancel();
            }
            else {
                logger.info("waiting for all subtasks to finish, taskId={}", getTaskId());
                final WaitResult waitResult = waitTaskEnd();
                if (waitResult.isAborted()) {
                    throw taskInfoContext.getAbortException();
                }

                if (waitResult.isCanceled()) {
                    taskCancel();
                }
                else if (waitResult.isTimeout()) {
                    taskTimeout();
                }
                else {
                    taskFinished();
                }
            }
        }
        catch (Throwable e) {
            logger.warn("do task failed:taskId=" + getTaskId(), e);
            discardSubTask();
            try {
                taskInfoContext.waitAllSubTaskFinish();
            }
            catch (Exception ex) {
                logger.warn("failed to wait all subTask finish", ex);
            }
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    e.toString(), taskInfoContext.getSuccessCount(),
                    taskInfoContext.getFailedCount(), taskInfoContext.getProgress());
        }
        finally {
            closeCursor(cursor);
        }
    }

    private long getStartTime() {
        Number startTime = (Number) taskInfo.get(FieldName.Task.FIELD_START_TIME);
        return startTime.longValue();
    }

    private boolean isTimeOut() {
        if (maxExecTime > 0) {
            Date now = new Date();
            return now.getTime() - taskStartTime > maxExecTime;
        }
        return false;
    }

    private void taskFinished() throws Exception {
        taskInfoContext.waitAllSubTaskFinish();
        taskComplete();
        finishTaskAndAsyncRedo(getTaskId(), taskInfoContext.getSuccessCount(),
                taskInfoContext.getFailedCount());
        logger.info("task have finished:taskId={}", taskId);
    }

    private void taskCancel() throws Exception {
        logger.info("begin to cancel task:taskId={}", getTaskId());
        discardSubTask();
        taskInfoContext.waitAllSubTaskFinish();
        taskComplete();
        logger.info("task have been canceled:taskId={}", getTaskId());
        updateTaskStopTimeAndAsyncRedo(getTaskId(), taskInfoContext.getSuccessCount(),
                taskInfoContext.getFailedCount(), taskInfoContext.getProgress());
    }

    private void taskTimeout() throws Exception {
        logger.info("begin to interrupt task:taskId={}", getTaskId());
        discardSubTask();
        taskInfoContext.waitAllSubTaskFinish();
        taskComplete();
        logger.warn(
                "task have been interrupted because of timeout:taskId={},startTime={},now={},maxExecTime={}ms",
                getTaskId(), taskStartTime, new Date(), maxExecTime);
        abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_TIMEOUT, "timeout",
                taskInfoContext.getSuccessCount(), taskInfoContext.getFailedCount(),
                taskInfoContext.getProgress());
    }

    private WaitResult waitTaskEnd() throws Exception {
        final WaitResult waitResult = new WaitResult();
        taskInfoContext.waitAllSubTaskFinish(Long.MAX_VALUE, 1000,
                new ScmTaskInfoContext.WaitCallback() {
                    @Override
                    public boolean shouldContinueWait(long waitingTime, int waitingCount)
                            throws ScmServerException {
                        if (taskInfoContext.isAborted()) {
                            waitResult.setAborted(true);
                            return false;
                        }
                        if (maxExecTime > 0
                                && (new Date().getTime() - taskStartTime) > maxExecTime) {
                            waitResult.setTimeout(true);
                            return false;
                        }
                        if (CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == getTaskRunningFlag(
                                getTaskId())) {
                            waitResult.setCanceled(true);
                            return false;
                        }
                        return true;
                    }
                });
        return waitResult;
    }

    private void discardSubTask() {
        int purgeSubTaskCount = ScmTaskManager.getInstance().purgeTask(getTaskId());
        if (purgeSubTaskCount > 0) {
            taskInfoContext.reduceActiveCount(purgeSubTaskCount);
        }
    }

    public void submitSubTask(ScmFileSubTask scmFileSubTask) throws ScmSystemException {
        try {
            scmFileSubTask.taskSubmitted();
            ScmJobManager.getInstance().executeShortTimeTask(scmFileSubTask);
        }
        catch (Exception e) {
            scmFileSubTask.taskDestroyed();
            throw new ScmSystemException("failed to submit subTask,taskId=" + getTaskId()
                    + ", fileId=" + scmFileSubTask.getFileId(), e);
        }
    }

    @Override
    protected long getEstimateCount() {
        return taskInfoContext.getEstimateCount();
    }

    @Override
    protected long getActualCount() {
        return taskInfoContext.getActualCount();
    }

    public long getTaskStartTime() {
        return taskStartTime;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    @Override
    public BSONObject getTaskInfo() {
        return taskInfo;
    }

    static class WaitResult {
        private boolean aborted;
        private boolean canceled;
        private boolean timeout;

        public boolean isAborted() {
            return aborted;
        }

        public void setAborted(boolean aborted) {
            this.aborted = aborted;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        public boolean isTimeout() {
            return timeout;
        }

        public void setTimeout(boolean timeout) {
            this.timeout = timeout;
        }
    }
}
