package com.sequoiacm.om.omserver.module;

public class OmServiceInstanceInfo {
    private String host;
    private int port;
    private String serviceName;
    private String region;
    private String zone;
    private boolean isContentServer;
    private boolean isRootSite;

    public boolean isContentServer() {
        return isContentServer;
    }

    public void setContentServer(boolean isContentServer) {
        this.isContentServer = isContentServer;
    }

    public boolean isRootSite() {
        return isRootSite;
    }

    public void setRootSite(boolean isRootSite) {
        this.isRootSite = isRootSite;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

}
