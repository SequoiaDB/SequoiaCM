package com.sequoiacm.tools.exec;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.element.ScmNodeStatus;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmWindowsExecutorImpl implements ScmExecutor {

    @Override
    public void startNode(String jarPath, String springConfigLocation, String loggingConfig,
            String errorLogPath, String options, String workingDir) throws ScmToolsException {
        // TODO Auto-generated method stub
        throw new ScmToolsException("Unsupported platform", ScmExitCode.SYSTEM_ERROR);
    }

    @Override
    public void stopNode(int pid, boolean isForce) throws ScmToolsException {
        // TODO Auto-generated method stub
        throw new ScmToolsException("Unsupported platform", ScmExitCode.SYSTEM_ERROR);
    }

    @Override
    public ScmNodeStatus getNodeStatus() throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.SYSTEM_ERROR);
        // TODO Auto-generated method stub jps

    }

    @Override
    public void execShell(String cmd) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.SYSTEM_ERROR);
        // TODO Auto-generated method stub exec
    }
}
