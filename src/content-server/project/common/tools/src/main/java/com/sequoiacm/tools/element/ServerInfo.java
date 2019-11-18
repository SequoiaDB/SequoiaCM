package com.sequoiacm.tools.element;

public class ServerInfo {
    public ServerInfo(int pid, int port, String confPort) {
        super();
        this.pid = pid;
        this.port = port;
        this.confPort = confPort;
    }

    private int pid;
    private int port;
    private String confPort;

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConfPort() {
        return confPort;
    }

    public void setConfPort(String confPort) {
        this.confPort = confPort;
    }
}
