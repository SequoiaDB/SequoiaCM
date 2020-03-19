package com.sequoiacm.s3.tools.command;

import com.sequoiacm.s3.tools.exception.ScmToolsException;

public interface ScmTool {
    public void process(String[] args) throws ScmToolsException;

    public void printHelp(boolean isFullHelp) throws ScmToolsException;
}
