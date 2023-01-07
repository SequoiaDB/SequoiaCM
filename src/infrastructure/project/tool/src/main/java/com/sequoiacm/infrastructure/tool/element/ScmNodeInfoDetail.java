package com.sequoiacm.infrastructure.tool.element;

public class ScmNodeInfoDetail {
    public static final int NOT_RUNNING = -1;
    private ScmNodeInfo nodeInfo;
    private int pid = NOT_RUNNING;

    public ScmNodeInfoDetail(ScmNodeInfo nodeInfo, int pid) {
        this.nodeInfo = nodeInfo;
        this.pid = pid;
    }

    public ScmNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public int getPid() {
        return pid;
    }

    @Override
    public String toString() {
        return "ScmNodeInfoDetail{" + "nodeInfo=" + nodeInfo + ", pid=" + pid + '}';
    }
}
