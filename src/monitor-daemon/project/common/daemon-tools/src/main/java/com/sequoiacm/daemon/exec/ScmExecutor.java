package com.sequoiacm.daemon.exec;

import com.sequoiacm.daemon.element.ScmCmdResult;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.util.List;

public interface ScmExecutor {
    abstract ScmCmdResult execCmd(String cmd) throws ScmToolsException;

    abstract void killPid(int pid, boolean isForced) throws ScmToolsException;

    abstract List<Integer> getPid(String match) throws ScmToolsException;

}
