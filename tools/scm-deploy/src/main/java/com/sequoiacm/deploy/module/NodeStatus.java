package com.sequoiacm.deploy.module;

public class NodeStatus {
    private boolean isStart;
    private String port;

    public NodeStatus(boolean isStart) {
        this.isStart = isStart;
    }
    public NodeStatus(String port) {
        this.port = port;
    }

    public NodeStatus(String port, boolean isStart) {
        this.isStart = isStart;
        this.port = port;
    }


    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    public String getPort() {
        return port;
    }
}
