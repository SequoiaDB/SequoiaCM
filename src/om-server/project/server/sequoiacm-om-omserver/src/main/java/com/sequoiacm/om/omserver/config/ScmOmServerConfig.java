package com.sequoiacm.om.omserver.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.omserver")
public class ScmOmServerConfig {
    public static final int DEFAULT_READ_TIMEOUT = 20000;
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    private String region = "DEFAULT_REGION";
    private String zone = "DEFAULT_ZONE";

    private List<String> gateway;
    private int readTimeout = DEFAULT_READ_TIMEOUT; // ms
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT; // ms

    private boolean onlyConnectLocalRegionServer = false;
    private int sessionKeepAliveTime = 900; // second

    private int cacheRefreshInterval = 120; // second

    private int healthCheckInterval = 10; // second

    private int instanceUpdateInterval = 30; // second

    private int serviceCenterUrlsUpdateInterval = 60; // second

    public int getCacheRefreshInterval() {
        return cacheRefreshInterval;
    }

    public void setCacheRefreshInterval(int cacheRefreshInterval) {
        this.cacheRefreshInterval = cacheRefreshInterval;
    }

    public List<String> getGateway() {
        return gateway;
    }

    public void setGateway(List<String> gateway) {
        this.gateway = gateway;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getSessionKeepAliveTime() {
        return sessionKeepAliveTime;
    }

    public void setSessionKeepAliveTime(int sessionKeepAliveTime) {
        if (sessionKeepAliveTime < 1) {
            throw new IllegalArgumentException(
                    "invalid sessionKeepAliveTime:" + sessionKeepAliveTime);
        }
        this.sessionKeepAliveTime = sessionKeepAliveTime;
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

    public boolean isOnlyConnectLocalRegionServer() {
        return onlyConnectLocalRegionServer;
    }

    public void setOnlyConnectLocalRegionServer(boolean onlyConnectLocalRegionServer) {
        this.onlyConnectLocalRegionServer = onlyConnectLocalRegionServer;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(int healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public int getInstanceUpdateInterval() {
        return instanceUpdateInterval;
    }

    public void setInstanceUpdateInterval(int instanceUpdateInterval) {
        this.instanceUpdateInterval = instanceUpdateInterval;
    }

    public int getServiceCenterUrlsUpdateInterval() {
        return serviceCenterUrlsUpdateInterval;
    }

    public void setServiceCenterUrlsUpdateInterval(int serviceCenterUrlsUpdateInterval) {
        this.serviceCenterUrlsUpdateInterval = serviceCenterUrlsUpdateInterval;
    }
}
