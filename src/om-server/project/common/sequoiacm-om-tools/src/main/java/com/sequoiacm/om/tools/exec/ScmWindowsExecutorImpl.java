package com.sequoiacm.om.tools.exec;

import com.sequoiacm.om.tools.element.ScmNodeStatus;
import com.sequoiacm.om.tools.exception.ScmExitCode;
import com.sequoiacm.om.tools.exception.ScmToolsException;

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

}
