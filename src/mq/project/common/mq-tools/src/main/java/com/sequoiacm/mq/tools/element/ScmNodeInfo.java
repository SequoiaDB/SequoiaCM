package com.sequoiacm.mq.tools.element;


public class ScmNodeInfo {
    private String confPath;
    private ScmNodeType nodeType;
    private int port;

    public ScmNodeInfo(String confPath, ScmNodeType nodeType, int port) {
        super();
        this.confPath = confPath;
        this.nodeType = nodeType;
        this.port = port;
    }

    public String getConfPath() {
        return confPath;
    }

    public void setConfPath(String confPath) {
        this.confPath = confPath;
    }

    public ScmNodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(ScmNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return nodeType + "(" + port + ")";
    }
}
