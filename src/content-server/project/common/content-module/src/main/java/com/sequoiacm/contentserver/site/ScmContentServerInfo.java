package com.sequoiacm.contentserver.site;


public class ScmContentServerInfo {
    private int id;
    private String name;
    private ScmSite site;
    private String hostName;
    private int port;
    private int type;

    public ScmContentServerInfo(int id, String name, ScmSite site, String hostName, int port,
            int type) {
        this.id = id;
        this.name = name;
        this.site = site;
        this.hostName = hostName;
        this.port = port;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ScmSite getSite() {
        return site;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public int getType() {
        return type;
    }

}
