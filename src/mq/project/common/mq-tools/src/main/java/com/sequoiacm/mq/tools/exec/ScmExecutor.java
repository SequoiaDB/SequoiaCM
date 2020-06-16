package com.sequoiacm.mq.tools.exec;

import com.sequoiacm.mq.tools.element.ScmNodeStatus;
import com.sequoiacm.mq.tools.exception.ScmToolsException;

public interface ScmExecutor {
    abstract void startNode(String jarPath, String springConfigLocation, String loggingConfig,
            String errorLogPath, String options) throws ScmToolsException;

    abstract void stopNode(int pid, boolean isForce) throws ScmToolsException;

    abstract ScmNodeStatus getNodeStatus() throws ScmToolsException;

}
