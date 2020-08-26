package com.sequoiacm.fulltext.tools.command;

import com.sequoiacm.fulltext.tools.exception.ScmToolsException;

public interface ScmTool {
    public void process(String[] args) throws ScmToolsException;

    public void printHelp(boolean isFullHelp) throws ScmToolsException;
}
