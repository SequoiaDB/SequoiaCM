package com.sequoiacm.schedule.tools.exec;

import com.sequoiacm.schedule.tools.element.ScmNodeStatus;
import com.sequoiacm.schedule.tools.exception.ScmToolsException;

public interface ScmExecutor {
    abstract void startNode(String jarPath, String springConfigLocation, String loggingConfig,
            String errorLogPath, String options) throws ScmToolsException;

    abstract void stopNode(int pid, boolean isForce) throws ScmToolsException;

    abstract ScmNodeStatus getNodeStatus() throws ScmToolsException;

}
