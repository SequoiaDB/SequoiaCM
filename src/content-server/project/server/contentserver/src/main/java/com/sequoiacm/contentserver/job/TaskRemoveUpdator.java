package com.sequoiacm.contentserver.job;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentServer;

public class TaskRemoveUpdator implements TaskUpdator {

    private String taskId;
    public TaskRemoveUpdator(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void doUpdate() throws ScmServerException {
        ScmContentServer.getInstance().getMetaService().deleteTask(taskId);
    }

}
