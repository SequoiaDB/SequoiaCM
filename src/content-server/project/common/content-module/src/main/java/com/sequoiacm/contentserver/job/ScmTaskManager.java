package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.ScmServer;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ScmTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskManager.class);

    private static volatile ScmTaskManager taskManager;

    private Map<String, ScmTaskBase> taskMap = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock();
    private ScmJobUpdateTask updateTaskStatusJob = new ScmJobUpdateTask();

    @Autowired
    public ScmTaskManager(ScmJobManager scmJobManager) throws Exception {
        try {
            scmJobManager.schedule(updateTaskStatusJob, 0);
            ScmTaskManager.taskManager = this;
        }
        catch (Exception e) {
            logger.error("schedule update task job failed", e);
            throw e;
        }
    }

    public static ScmTaskManager getInstance() {
        checkState();
        return taskManager;
    }

    private static void checkState() {
        if (taskManager == null) {
            throw new RuntimeException("ScmTaskManager is not initialized");
        }
    }

    public ScmTaskBase createTask(BSONObject info, boolean isAsyncCountFile)
            throws ScmServerException {
        checkState();
        ScmTaskBase task = null;

        // task will be added to ScmTaskManager's map in
        // constructor(ScmTaskTransferFile)
        int type = (int) info.get(FieldName.Task.FIELD_TYPE);
        switch (type) {
            case CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE:
                task = new ScmTaskTransferFile(this, info, isAsyncCountFile);
                break;

            case CommonDefine.TaskType.SCM_TASK_CLEAN_FILE:
                task = new ScmTaskCleanFile(this, info, isAsyncCountFile);
                break;

            case CommonDefine.TaskType.SCM_TASK_MOVE_FILE:
                task = new ScmTaskMoveFile(this, info, isAsyncCountFile);
                break;

            case CommonDefine.TaskType.SCM_TASK_RECYCLE_SPACE:
                task = new ScmSpaceRecycleTask(this, info);
                break;

            default:
                throw new ScmInvalidArgumentException("unrecognized task type:type=" + type);
        }

        return task;
    }

    public void addTask(ScmTaskBase task) {
        checkState();
        lock.lock();
        try {
            taskMap.put(task.getTaskId(), task);
        }
        finally {
            lock.unlock();
        }
    }

    public void delTask(String taskId) {
        checkState();
        lock.lock();
        try {
            taskMap.remove(taskId);
        }
        finally {
            lock.unlock();
        }
    }

    public ScmTaskBase getTask(String taskId) {
        checkState();
        lock.lock();
        try {
            return taskMap.get(taskId);
        }
        finally {
            lock.unlock();
        }
    }

    public int getTaskCount() {
        checkState();
        lock.lock();
        try {
            return taskMap.size();
        }
        finally {
            lock.unlock();
        }
    }

    public void restore() throws ScmServerException {
        checkState();
        // abort init & running task(taskType=transfer_file & clean_file)
        int mainSiteId = ScmContentModule.getInstance().getMainSite();
        int serverId = ScmServer.getInstance().getContentServerInfo().getId();
        AbortTask(mainSiteId, serverId);

        // fill stop time is null
        fillStopTime(mainSiteId, serverId);
    }

    private void fillStopTime(int mainSiteId, int serverId) throws ScmServerException {
        Date date = new Date();
        ScmContentModule.getInstance().getMetaService().setAllStopTime(serverId, date);
    }

    private void AbortTask(int mainSiteId, int serverId) throws ScmServerException {
        Date date = new Date();
        ScmContentModule.getInstance().getMetaService().abortAllTask(serverId, date);
    }

    public void addAsyncTaskUpdator(TaskUpdator updator) {
        checkState();
        updateTaskStatusJob.addUpdator(updator);
    }

    public int purgeTask(String taskId) {
        checkState();
        int count = 0;
        BlockingQueue<Runnable> taskQueue = ScmJobManager.getInstance().getTaskQueue();
        for (Runnable task : taskQueue) {
            if (task instanceof ScmFileSubTask) {
                ScmFileSubTask subTask = (ScmFileSubTask) task;
                if (subTask.getTaskId().equals(taskId) && taskQueue.remove(task)) {
                    count++;
                }
            }
        }
        return count;
    }

}
