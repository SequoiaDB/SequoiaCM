package com.sequoiacm.daemon.element;

import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;

import java.io.Serializable;

public class ScmNodeInfo implements Serializable {
    private ScmServerScriptEnum serverType;
    private int port;
    private String status;
    // /opt/sequoiacm/sequoiacm-config/conf/config-server/8190/application.properties
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
        return "ScmNodeInfo{" +
                DaemonDefine.SERVER_TYPE + "=" + serverType + "," +
                DaemonDefine.PORT + "=" + port + "," +
                DaemonDefine.STATUS + "=" + status + "," +
                DaemonDefine.CONF_PATH + "=" + confPath +
                "}";
    }
}
