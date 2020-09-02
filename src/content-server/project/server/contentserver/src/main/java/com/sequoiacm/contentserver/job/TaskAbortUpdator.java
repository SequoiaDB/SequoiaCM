package com.sequoiacm.contentserver.job;

import java.util.Date;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentServer;

public class TaskAbortUpdator implements TaskUpdator {

    private String taskId;
    private String detail;
    private long successCount;
    private long failedCount;
    private int progress;
    private int flag;

    public TaskAbortUpdator(String taskId, int flag, String detail, long successCount,
            long failedCount, int progress) {
        this.taskId = taskId;
        this.detail = detail;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.progress = progress;
        this.flag = flag;
    }

    @Override
    public void doUpdate() throws ScmServerException {
        ScmContentServer.getInstance().getMetaService().abortTask(taskId, flag, detail,
                new Date(), progress, successCount, failedCount);
    }

    @Override
    public String getTaskId() {
        return taskId;
    }
}
