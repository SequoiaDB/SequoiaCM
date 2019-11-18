package com.sequoiacm.cloud.adminserver.model;

public class ContentServerInfo {
    private int id;
    private String name;
    private String hostname;
    private int port;
    private int siteId;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getSiteId() {
        return siteId;
    }
    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }    
    
    public String getNodeUrl() {
        return hostname + ":" + port;
    }
    
    @Override
    public String toString() {
        return "{"
                + "id:" + id + ","
                + "name:" + name + ","
                + "hostname:" + hostname + ","
                + "port:" + port + ","
                + "siteId:" + siteId
                + "}";
    }
}
