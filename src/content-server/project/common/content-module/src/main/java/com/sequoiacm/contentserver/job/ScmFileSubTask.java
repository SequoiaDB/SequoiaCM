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
        ScmLock fileReadLock = null;
        String fileId = null;
        try {
            fileId = (String) fileInfo.get(FieldName.FIELD_CLFILE_ID);
            ScmLockPath lockPath = ScmLockPathFactory
                    .createFileLockPath(getWorkspaceInfo().getName(), fileId);
            fileReadLock = ScmLockManager.getInstance().acquiresReadLock(lockPath);
            doTask();
        }
        catch (Throwable e) {
            taskInfoContext.subTaskAbort(e);
            logger.error("failed execute subTask, taskId={}, fileId={}", getTaskId(), fileId, e);
        }
        finally {
            if (fileReadLock != null) {
                fileReadLock.unlock();
            }
            taskDestroyed();
        }
    }

    protected abstract void doTask() throws ScmServerException;

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
