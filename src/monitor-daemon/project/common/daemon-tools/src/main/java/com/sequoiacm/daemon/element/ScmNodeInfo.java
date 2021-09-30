package com.sequoiacm.daemon.element;

import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;

import java.io.Serializable;

public class ScmNodeInfo implements Serializable {
    private ScmServerScriptEnum serverType;
    private int port;
    private String status;
    private String confPath;

    public ScmServerScriptEnum getServerType() {
        return serverType;
    }

    public void setServerType(ScmServerScriptEnum serverType) {
        this.serverType = serverType;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfPath() {
        return confPath;
    }

    public void setConfPath(String confPath) {
        this.confPath = confPath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScmNodeInfo:");
        if (serverType != null) {
            sb.append("type=").append(serverType.getType()).append(",");
        }
        if (port != 0) {
            sb.append("port=").append(port).append(",");
        }
        if (status != null) {
            sb.append("status=").append(status).append(",");
        }
        if (confPath != null) {
            sb.append("confPath=").append(confPath).append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }
}
