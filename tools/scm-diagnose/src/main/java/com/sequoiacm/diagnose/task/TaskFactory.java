package com.sequoiacm.diagnose.task;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;


public interface TaskFactory {

    Runnable createTask(ExecutionContext context) throws ScmToolsException;
}
