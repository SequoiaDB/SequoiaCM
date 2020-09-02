package com.sequoiacm.contentserver.job;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.site.ScmContentServer;

public abstract class ScmTaskBase extends ScmBackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskBase.class);

    protected ScmTaskManager mgr = null;

    public ScmTaskBase(ScmTaskManager mgr) {
        this.mgr = mgr;
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

    public void startTask() throws ScmServerException {
        ScmContentServer.getInstance().getMetaService().startTask(getTaskId(), new Date(),
                getEstimateCount(), getActualCount());
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
        ScmContentServer.getInstance().getMetaService().cancelTask(taskId, detail);
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
            ScmJobManager.getInstance().schedule(this, 0);
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
            throw new ScmSystemException("start job failed",
                    e);
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
            //NOTE:should not come here!
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

    public abstract void _stop();

    public abstract void _runTask();

    protected abstract long getEstimateCount() throws ScmServerException;

    protected abstract long getActualCount() throws ScmServerException;
}
