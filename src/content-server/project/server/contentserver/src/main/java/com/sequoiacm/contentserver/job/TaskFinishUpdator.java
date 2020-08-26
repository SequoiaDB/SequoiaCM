package com.sequoiacm.contentserver.job;

import java.util.Date;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentServer;

public class TaskFinishUpdator implements TaskUpdator {
    private String taskId;
    private long failedCount;
    private long successCount;

    public TaskFinishUpdator(String taskId, long successCount, long failedCount) {
        this.taskId = taskId;
        this.successCount = successCount;
        this.failedCount = failedCount;
    }

    @Override
    public void doUpdate() throws ScmServerException {
        ScmContentServer.getInstance().getMetaService().finishTask(taskId, new Date(),
                successCount, failedCount);
    }

    @Override
    public String getTaskId() {
        return taskId;
    }
}
