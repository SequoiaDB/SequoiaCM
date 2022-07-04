package com.sequoiacm.cloud.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConfigurationProperties(prefix = "scm.uploadForward")
public class CustomForwardConfig {
    
    private final static Logger logger = LoggerFactory.getLogger(CustomForwardConfig.class);

    private Environment environment;

    private static final String PROPERTY_RIBBON_READ_TIMEOUT = "ribbon.ReadTimeout";
    private static final String PROPERTY_RIBBON_CONNECT_TIMEOUT = "ribbon.ConnectTimeout";
    private static final String PROPERTY_UPLOAD_SOCKET_TIMEOUT = "scm.uploadForward.socketTimeout";
    private static final String PROPERTY_UPLOAD_CONNECT_TIMEOUT = "scm.uploadForward.connectTimeout";

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

    private int bufferSize = 1024 * 4;
    
    public CustomForwardConfig(Environment environment) {
        this.environment = environment;
        this.connectTimeout = getPreferredValue(PROPERTY_UPLOAD_CONNECT_TIMEOUT,
                PROPERTY_RIBBON_CONNECT_TIMEOUT, connectTimeout);
        this.socketTimeout = getPreferredValue(PROPERTY_UPLOAD_SOCKET_TIMEOUT,
                PROPERTY_RIBBON_READ_TIMEOUT, socketTimeout);
    }

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

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    private int getPreferredValue(String firstPropertyKey, String secondPropertyKey,
            int defaultValue) {
        String firstPropertyValue = environment.getProperty(firstPropertyKey);
        if (firstPropertyValue != null) {
            return Integer.parseInt(firstPropertyValue);
        }
        String secondPropertyValue = environment.getProperty(secondPropertyKey);
        if (secondPropertyValue != null) {
            logger.info("UploadForwardConfig: {} is not exist, use {}, value:{}", firstPropertyKey,
                    secondPropertyKey, secondPropertyValue);
            return Integer.parseInt(secondPropertyValue);
        }
        return defaultValue;
    }

}
