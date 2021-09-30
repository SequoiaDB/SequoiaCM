package com.sequoiacm.infrastructure.tool.exec;

import com.sequoiacm.infrastructure.tool.element.ScmNodeStatus;
import com.sequoiacm.infrastructure.tool.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.util.List;

public class ScmWindowsExecutorImpl implements ScmExecutor {

    @Override
    public void startNode(String jarName, String springConfigLocation, String loggingConfig,
            String errorLogPath, String options) throws ScmToolsException {
        // TODO Auto-generated method stub
        throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
    }

    @Override
    public void stopNode(int pid, boolean isForce) throws ScmToolsException {
        // TODO Auto-generated method stub
        throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
    }

    @Override
    public ScmNodeStatus getNodeStatus() throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
        // TODO Auto-generated method stub jps

    }

    @Override
    public void execShell(String cmd) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
        // TODO Auto-generated method stub exec
    }
}
