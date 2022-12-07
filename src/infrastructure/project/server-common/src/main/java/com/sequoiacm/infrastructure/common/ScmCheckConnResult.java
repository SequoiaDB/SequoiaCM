package com.sequoiacm.infrastructure.common;

public class ScmCheckConnResult {
    private boolean connected;
    private String service;
    private String host;
    private Integer port;
    private String region;
    private String zone;
    private String ip;

    public ScmCheckConnResult(String service, String ip, String host, int port, boolean connected,
            String region, String zone) {
        this.connected = connected;
        this.service = service;
        this.host = host;
        this.port = port;
        this.ip = ip;
        this.region = region;
        this.zone = zone;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getService() {
        return service;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getRegion() {
        return region;
    }

    public String getZone() {
        return zone;
    }

    public String getIp() {
        return ip;
    }

}
