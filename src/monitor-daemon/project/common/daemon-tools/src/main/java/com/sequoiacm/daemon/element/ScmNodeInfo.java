package com.sequoiacm.daemon.element;

import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.manager.ScmDaemonMgr;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiadb.datasource.DatasourceOptions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
        return "ScmNodeInfo{" +
                DaemonDefine.SERVER_TYPE + "=" + serverType + "," +
                DaemonDefine.PORT + "=" + port + "," +
                DaemonDefine.STATUS + "=" + status + "," +
                DaemonDefine.CONF_PATH + "=" + confPath +
                "}";
    }
}
