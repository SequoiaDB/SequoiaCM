package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScmFileSubTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ScmFileSubTask.class);

    protected BSONObject fileInfo;
    protected ScmTaskInfoContext taskInfoContext;
    private ScmWorkspaceInfo wsInfo;
    private volatile boolean isSubmitted;
    private volatile boolean isDestroyed;
    private static final int TASK_MAX_RETRY_COUNT = 3;

    public ScmFileSubTask(BSONObject fileInfo, ScmWorkspaceInfo wsInfo,
            ScmTaskInfoContext taskInfoContext) {
        this.fileInfo = fileInfo;
        this.wsInfo = wsInfo;
        this.taskInfoContext = taskInfoContext;
    }

    public synchronized void taskSubmitted() {
        if (!isSubmitted && !isDestroyed) {
            isSubmitted = true;
            taskInfoContext.incrementActiveCount();
        }
    }

    public synchronized void taskDestroyed() {
        if (!isDestroyed && isSubmitted) {
            isDestroyed = true;
            taskInfoContext.decrementActiveCount();
        }
    }

    @Override
    public void run() {
        String fileId = (String) fileInfo.get(FieldName.FIELD_CLFILE_ID);
        try {
            DoTaskRes res = runTask(fileId);
            if (res.getRes() != ScmDoFileRes.ABORT) {
                taskInfoContext.subTaskFinish(res.getRes());
                return;
            }
            logger.error("Execute task failed, taskId={}, fileId={}", getTaskId(), fileId,
                    res.getE());
            // abort 结果的需要进行重试
            int retryCount = 1;
            while (retryCount <= TASK_MAX_RETRY_COUNT) {
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    // ignore
                }
                logger.info("Starting to retry the task, taskId={}, fileId={}, ({}/{})",
                        getTaskId(), fileId, retryCount, TASK_MAX_RETRY_COUNT);
                res = runTask(fileId);
                if (res.getRes() != ScmDoFileRes.ABORT) {
                    taskInfoContext.subTaskFinish(res.getRes());
                    return;
                }
                logger.error("Execute task failed, taskId={}, fileId={}", getTaskId(), fileId,
                        res.getE());
                if (retryCount == TASK_MAX_RETRY_COUNT) {
                    // 三次都失败，设置状态为 abort
                    taskInfoContext.subTaskAbort(res.getE());
                    return;
                }
                ++retryCount;
            }
        }
        catch (Throwable e) {
            // 有异常，直接失败
            logger.error("failed execute subTask, taskId={}, fileId={}", getTaskId(),
                    fileId, e);
            taskInfoContext.subTaskAbort(e);
        }
        finally {
            taskDestroyed();
        }
    }

    private DoTaskRes runTask(String fileId) throws Exception {
        ScmLock fileReadLock = null;
        try {
            ScmLockPath lockPath = ScmLockPathFactory
                    .createFileLockPath(getWorkspaceInfo().getName(), fileId);
            fileReadLock = ScmLockManager.getInstance().acquiresReadLock(lockPath);
            SchTaskSlowLogOperator.getInstance().doTaskBefore();
            return doTask();
        }
        finally {
            SchTaskSlowLogOperator.getInstance().doTaskAfter(getTaskId(), fileId);
            if (fileReadLock != null) {
                fileReadLock.unlock();
            }
        }
    }

    class DoTaskRes {
        private Throwable e;
        private ScmDoFileRes res;

        public DoTaskRes(Throwable e, ScmDoFileRes res) {
            this.e = e;
            this.res = res;
        }

        public Throwable getE() {
            return e;
        }

        public ScmDoFileRes getRes() {
            return res;
        }
    }

    protected abstract DoTaskRes doTask() throws ScmServerException;

    public String getTaskId() {
        return taskInfoContext.getTask().getTaskId();
    }

    public String getFileId() {
        return (String) fileInfo.get(FieldName.FIELD_CLFILE_ID);
    }

    public ScmTaskFile getTask() {
        return (ScmTaskFile) taskInfoContext.getTask();
    }

    public ScmWorkspaceInfo getWorkspaceInfo() {
        return wsInfo;
    }

}
