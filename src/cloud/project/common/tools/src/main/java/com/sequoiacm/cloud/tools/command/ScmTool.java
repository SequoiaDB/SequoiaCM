package com.sequoiacm.cloud.tools.command;

import com.sequoiacm.cloud.tools.exception.ScmToolsException;

public interface ScmTool {
    public void process(String[] args) throws ScmToolsException;

    public void printHelp(boolean isFullHelp) throws ScmToolsException;
}
