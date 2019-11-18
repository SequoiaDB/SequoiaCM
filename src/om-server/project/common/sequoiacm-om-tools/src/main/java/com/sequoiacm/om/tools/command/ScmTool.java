package com.sequoiacm.om.tools.command;

import com.sequoiacm.om.tools.exception.ScmToolsException;

public interface ScmTool {
    public void process(String[] args) throws ScmToolsException;

    public void printHelp(boolean isFullHelp) throws ScmToolsException;
}
