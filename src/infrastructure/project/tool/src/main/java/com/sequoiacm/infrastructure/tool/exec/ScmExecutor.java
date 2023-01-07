package com.sequoiacm.infrastructure.tool.exec;

import com.sequoiacm.infrastructure.tool.element.ScmNodeStatus;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.util.List;

public interface ScmExecutor {
    abstract void startNode(String jarPath, String springConfigLocation, String loggingConfig,
            String errorLogPath, String options, String workingDir) throws ScmToolsException;

    abstract void stopNode(int pid, boolean isForce) throws ScmToolsException;

    abstract ScmNodeStatus getNodeStatus(ScmNodeType nodeType) throws ScmToolsException;

    abstract void execShell(String cmd) throws ScmToolsException;
}
