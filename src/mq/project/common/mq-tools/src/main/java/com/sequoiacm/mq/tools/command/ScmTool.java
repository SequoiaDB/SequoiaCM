package com.sequoiacm.mq.tools.command;

import com.sequoiacm.mq.tools.exception.ScmToolsException;

public interface ScmTool {
    public void process(String[] args) throws ScmToolsException;

    public void printHelp(boolean isFullHelp) throws ScmToolsException;
}
