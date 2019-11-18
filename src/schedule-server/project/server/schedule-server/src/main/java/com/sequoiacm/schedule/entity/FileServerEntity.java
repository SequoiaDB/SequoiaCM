package com.sequoiacm.schedule.entity;

public class FileServerEntity {
    private int siteId;
    private int id;
    private String name;
    private String hostName;
    private int port;
    private int restPort;

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public int getId() {
        return id;
    }

    public void setId(int serverId) {
        this.id = serverId;
    }

    public String getName() {
        return name;
    }

    public void setName(String severName) {
        this.name = severName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;

        this.restPort = port;
    }

    public int getRestPort() {
        return restPort;
    }
}
