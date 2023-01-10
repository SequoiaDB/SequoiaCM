package com.sequoiacm.client.core;

public class ScmTransitionApplyInfo {
    private String workspace;
    private boolean isCustomized;

    public ScmTransitionApplyInfo(String workspace,boolean isCustomized){
        this.workspace = workspace;
        this.isCustomized = isCustomized;
    }

    public String getWorkspace() {
        return workspace;
    }

    public boolean isCustomized() {
        return isCustomized;
    }
}
