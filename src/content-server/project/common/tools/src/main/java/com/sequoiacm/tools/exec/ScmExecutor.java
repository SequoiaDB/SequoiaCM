package com.sequoiacm.tools.exec;


import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.element.ScmNodeStatus;

public interface ScmExecutor {
    abstract void startNode(String springConfigLocation, String loggingConfig, String errorLogPath,
            String options) throws ScmToolsException;

    abstract void stopNode(int pid, boolean isForce) throws ScmToolsException;

    abstract ScmNodeStatus getNodeStatus() throws ScmToolsException;

}
