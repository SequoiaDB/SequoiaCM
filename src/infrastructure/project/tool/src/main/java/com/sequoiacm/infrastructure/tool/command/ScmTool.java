package com.sequoiacm.infrastructure.tool.command;


import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public abstract class ScmTool {
    private String tooName = null;
    
    public ScmTool(String tooName) {
        this.tooName = tooName;
    }

    public abstract void process(String[] args) throws ScmToolsException;

    public abstract void printHelp(boolean isFullHelp) throws ScmToolsException;

    public String getToolName() {
        return this.tooName;
    }
}
