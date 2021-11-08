package com.sequoiacm.daemon.exec;

import com.sequoiacm.daemon.element.ScmCmdResult;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.util.List;

public class ScmWindowsExecutorImpl implements ScmExecutor {

    @Override
    public ScmCmdResult execCmd(String cmd) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.SYSTEM_ERROR);
    }

    @Override
    public void killPid(int pid, boolean isForced) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.SYSTEM_ERROR);
    }

    @Override
    public List<Integer> getPid(String match) throws ScmToolsException {
        throw new ScmToolsException("Unsupported platform", ScmExitCode.SYSTEM_ERROR);
    }
}
