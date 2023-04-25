package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public abstract class ScmTaskBase extends ScmBackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskBase.class);

    protected ScmTaskManager mgr = null;

    protected ScmTaskInfoContext taskInfoContext;

    public ScmTaskBase(ScmTaskManager mgr) {
        this.mgr = mgr;
        taskInfoContext = new ScmTaskInfoContext(this);
    }

    public void abortTaskAndAsyncRedo(String taskId, int flag, String detail, long successCount,
            long failedCount, int progress) {
        TaskUpdator updator = new TaskAbortUpdator(taskId, flag, detail, successCount, failedCount,
                progress);
        try {
            updator.doUpdate();
        }
        catch (Exception e) {
            mgr.addAsyncTaskUpdator(updator);
            logger.warn("abort task failed:taskId=" + taskId, e);
        }
    }

    public boolean checkAndStartTask() throws ScmServerException {
        return ScmContentModule.getInstance().getMetaService().checkAndStartTask(getTaskId(),
                new Date(), getEstimateCount(), getActualCount());
    }

    public void updateTaskFileCount(String taskId, long estimateCount, long actualCount)
            throws ScmServerException {
        ScmContentModule.getInstance().getMetaService().updateTaskFileCount(taskId, estimateCount,
                actualCount);
    }

    public void updateTaskStopTimeAndAsyncRedo(String taskId, long successCount, long failedCount,
            int progress) {
        TaskUpdator updator = new TaskStopTimeUpdator(taskId, successCount, failedCount, progress);
        try {
            updator.doUpdate();
        }
        catch (Exception e) {
            mgr.addAsyncTaskUpdator(updator);
            logger.warn("update task failed:taskId=" + taskId, e);
        }
    }

    public static void startCancelTask(String taskId, String detail) throws ScmServerException {
        ScmContentModule.getInstance().getMetaService().cancelTask(taskId, detail);
    }

    public void finishTaskAndAsyncRedo(String taskId, long successCount, long failedCount) {
        TaskUpdator updator = new TaskFinishUpdator(taskId, successCount, failedCount);
        try {
            updator.doUpdate();
        }
        catch (Exception e) {
            mgr.addAsyncTaskUpdator(updator);
            logger.warn("update task failed:taskId=" + taskId, e);
        }
    }

    public final void start() throws ScmServerException {
        try {
            // 任务添加进等待队列，若队列满了，则添加失败
            ScmJobManager.getInstance().executeScheduleTask(this);
            mgr.addTask(this);
        }
        catch (ScmServerException e) {
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    "start job failed", 0, 0, 0);
            throw e;
        }
        catch (Exception e) {
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    "start job failed", 0, 0, 0);
            throw new ScmSystemException("start job failed: taskId=" + getTaskId(), e);
        }
    }

    public final void stop() {
        _stop();
    }

    @Override
    public final void _run() {
        try {
            _runTask();
        }
        catch (Throwable e) {
            // NOTE:should not come here!
            logger.error("run task failed:taskId=" + getTaskId(), e);
            abortTaskAndAsyncRedo(getTaskId(), CommonDefine.TaskRunningFlag.SCM_TASK_ABORT,
                    e.toString(), 0, 0, 0);
        }
        finally {
            mgr.delTask(getTaskId());
        }
    }

    @Override
    public final int getType() {
        return ServiceDefine.Job.JOB_TYPE_TASK;
    }

    @Override
    public long getPeriod() {
        return 0;
    }

    public abstract String getTaskId();

    public abstract int getTaskType();

    public abstract BSONObject getTaskInfo();

    public abstract void _stop();

    public abstract void _runTask();

    protected abstract long getEstimateCount() throws ScmServerException;

    protected abstract long getActualCount() throws ScmServerException;
}
