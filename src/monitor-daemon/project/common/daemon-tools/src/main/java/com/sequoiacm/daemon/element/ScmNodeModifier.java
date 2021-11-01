package com.sequoiacm.daemon.element;

public class ScmNodeModifier {

    private final String status;

    public ScmNodeModifier(String status) {
        this.status = status;
    }

    public void modifyNodeInfo(ScmNodeInfo nodeInfo) {
        nodeInfo.setStatus(status);
    }
}
