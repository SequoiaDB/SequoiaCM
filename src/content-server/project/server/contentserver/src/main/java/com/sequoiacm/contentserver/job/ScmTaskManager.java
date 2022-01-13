package com.sequoiacm.contentserver.job;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.ScmServer;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ScmTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmTaskManager.class);

    private static ScmTaskManager taskManager = new ScmTaskManager();

    private Map<String, ScmTaskBase> taskMap = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock();
    private ScmJobUpdateTask updateTaskStatusJob = new ScmJobUpdateTask();

    private ScmTaskManager() {
        try {
            ScmJobManager.getInstance().schedule(updateTaskStatusJob, 0);
        }
        catch (Exception e) {
            logger.error("schedule update task job failed", e);
            System.exit(-1);
        }
    }

    public static ScmTaskManager getInstance() {
        return taskManager;
    }

    public ScmTaskBase createTask(BSONObject info) throws ScmServerException {
        ScmTaskBase task = null;

        // task will be added to ScmTaskManager's map in
        // constructor(ScmTaskTransferFile)
        int type = (int) info.get(FieldName.Task.FIELD_TYPE);
        switch (type) {
            case CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE:
                task = new ScmTaskTransferFile(this, info);
                break;

            case CommonDefine.TaskType.SCM_TASK_CLEAN_FILE:
                task = new ScmTaskCleanFile(this, info);
                break;

            default:
                throw new ScmInvalidArgumentException("unrecognized task type:type=" + type);
        }

        return task;
    }

    public void addTask(ScmTaskBase task) {
        lock.lock();
        try {
            taskMap.put(task.getTaskId(), task);
        }
        finally {
            lock.unlock();
        }
    }

    public void delTask(String taskId) {
        lock.lock();
        try {
            taskMap.remove(taskId);
        }
        finally {
            lock.unlock();
        }
    }

    public ScmTaskBase getTask(String taskId) {
        lock.lock();
        try {
            return taskMap.get(taskId);
        }
        finally {
            lock.unlock();
        }
    }

    public int getTaskCount() {
        lock.lock();
        try {
            return taskMap.size();
        }
        finally {
            lock.unlock();
        }
    }

    public void restore() throws ScmServerException {
        // abort init & running task(taskType=transfer_file & clean_file)
        int mainSiteId = ScmContentServer.getInstance().getMainSite();
        int serverId = ScmServer.getInstance().getContentServerInfo().getId();
        AbortTask(mainSiteId, serverId);

        // fill stop time is null
        fillStopTime(mainSiteId, serverId);
    }

    private void fillStopTime(int mainSiteId, int serverId) throws ScmServerException {
        Date date = new Date();
        ScmContentServer.getInstance().getMetaService().setAllStopTime(serverId, date);
    }

    private void AbortTask(int mainSiteId, int serverId) throws ScmServerException {
        Date date = new Date();
        ScmContentServer.getInstance().getMetaService().abortAllTask(serverId, date);
    }

    public void addAsyncTaskUpdator(TaskUpdator updator) {
        updateTaskStatusJob.addUpdator(updator);
    }
}
