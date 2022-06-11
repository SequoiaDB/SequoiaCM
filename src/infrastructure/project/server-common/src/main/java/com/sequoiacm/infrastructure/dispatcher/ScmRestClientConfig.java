package com.sequoiacm.infrastructure.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scm.restClient")
public class ScmRestClientConfig {

    private long connectionTimeToLive = 15 * 60L;
    private int maxTotalConnections = 1000;
    private int maxPerRouteConnections = 50;
    private long connectionCleanerRepeatInterval = 30000;

    // connect timeout
    private int connectTimeout = 10000;

    // get a connection from pool.
    private int connectionRequestTimeout = -1;

    // socket read timeout
    private int socketTimeout = 30000;

    public long getConnectionTimeToLive() {
        return connectionTimeToLive;
    }

    public void setConnectionTimeToLive(long connectionTimeToLive) {
        this.connectionTimeToLive = connectionTimeToLive;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getMaxPerRouteConnections() {
        return maxPerRouteConnections;
    }

    public void setMaxPerRouteConnections(int maxPerRouteConnections) {
        this.maxPerRouteConnections = maxPerRouteConnections;
    }

    public long getConnectionCleanerRepeatInterval() {
        return connectionCleanerRepeatInterval;
    }

    public void setConnectionCleanerRepeatInterval(long connectionCleanerRepeatInterval) {
        this.connectionCleanerRepeatInterval = connectionCleanerRepeatInterval;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}
