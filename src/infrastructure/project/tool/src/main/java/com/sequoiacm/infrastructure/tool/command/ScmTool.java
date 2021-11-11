package com.sequoiacm.infrastructure.tool.command;


import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public abstract class ScmTool {
    private String tooName = null;
    private boolean isHidden = false;

    public ScmTool(String tooName) {
        this.tooName = tooName;
    }

    public ScmTool(String tooName, boolean isHidden) {
        this.tooName = tooName;
        this.isHidden = isHidden;
    }

    public abstract void process(String[] args) throws ScmToolsException;

    public abstract void printHelp(boolean isFullHelp) throws ScmToolsException;

    public String getToolName() {
        return this.tooName;
    }

    public boolean isHidden() {
        return isHidden;
    }
}
