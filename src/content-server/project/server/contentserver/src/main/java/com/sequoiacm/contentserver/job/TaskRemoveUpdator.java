package com.sequoiacm.contentserver.job;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentModule;

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
        ScmContentModule.getInstance().getMetaService().deleteTask(taskId);
    }

}
