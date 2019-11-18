package com.sequoiacm.contentserver.job;

import java.util.Date;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentServer;

public class TaskStopTimeUpdator implements TaskUpdator {

    private String taskId;
    private long successCount;
    private long failCount;
    private int progress;

    public TaskStopTimeUpdator(String taskId,long successCount,long failCount,int progress) {
        this.taskId = taskId;
        this.successCount = successCount;
        this.failCount = failCount;
        this.progress = progress;
    }

    @Override
    public void doUpdate() throws ScmServerException {
        ScmContentServer.getInstance().getMetaService().updateTaskStopTimeIfEmpty(taskId,
                new Date(), progress, successCount, failCount);
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

}
