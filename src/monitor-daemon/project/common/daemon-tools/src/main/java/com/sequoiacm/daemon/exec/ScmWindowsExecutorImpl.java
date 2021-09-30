package com.sequoiacm.daemon.exec;

import com.sequoiacm.daemon.element.ScmCmdResult;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmWindowsExecutorImpl implements ScmExecutor {

    @Override
    public ScmCmdResult execCmd(String cmd) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
    }

    @Override
    public void killPid(int pid, boolean isForced) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
    }

    @Override
    public int getPid(String match) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.UNSUPORT_PLATFORM);
    }
}
