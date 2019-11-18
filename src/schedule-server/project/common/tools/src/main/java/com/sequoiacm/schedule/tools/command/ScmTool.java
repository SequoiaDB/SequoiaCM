package com.sequoiacm.schedule.tools.command;

import com.sequoiacm.schedule.tools.exception.ScmToolsException;

public interface ScmTool {
    public void process(String[] args) throws ScmToolsException;

    public void printHelp(boolean isFullHelp) throws ScmToolsException;
}
