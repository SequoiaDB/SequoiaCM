package com.sequoiacm.om.omserver.module;

public class OmSrviceInstanceInfo {
    private String host;
    private int port;
    private String serviceName;
    private String regionl;
    private String zone;

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

    public String getRegionl() {
        return regionl;
    }

    public void setRegionl(String regionl) {
        this.regionl = regionl;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

}
