package com.sequoiacm.cloud.tools.exec;

import com.sequoiacm.cloud.tools.element.ScmNodeStatus;
import com.sequoiacm.cloud.tools.exception.ScmToolsException;

public interface ScmExecutor {
    abstract void startNode(String jarPath, String springConfigLocation, String loggingConfig,
            String errorLogPath, String options) throws ScmToolsException;

    abstract void stopNode(int pid, boolean isForce) throws ScmToolsException;

    abstract ScmNodeStatus getNodeStatus() throws ScmToolsException;

}
