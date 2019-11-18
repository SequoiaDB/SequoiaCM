package com.sequoiacm.tools.exec;


import com.sequoiacm.tools.element.ScmNodeStatus;
import com.sequoiacm.tools.exception.ScmToolsException;

public interface ScmExecutor {
    abstract void startNode(String springConfigLocation, String loggingConfig, String errorLogPath,
            String options) throws ScmToolsException;

    abstract void stopNode(int pid, boolean isForce) throws ScmToolsException;

    abstract ScmNodeStatus getNodeStatus() throws ScmToolsException;

}
