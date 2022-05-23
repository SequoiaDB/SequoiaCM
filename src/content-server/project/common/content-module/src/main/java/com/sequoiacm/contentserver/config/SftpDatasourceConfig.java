package com.sequoiacm.contentserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "scm.sftp")
@Configuration
public class SftpDatasourceConfig {

    private static final String KEY_CONNECT_TIMEOUT = "CONNECT_TIMEOUT";
    private static final String KEY_SOCKET_TIMEOUT = "SOCKET_TIMEOUT";
    private static final String KEY_SERVER_ALIVE_INTERVAL = "SERVER_ALIVE_INTERVAL";

    private int connectTimeout = 30000;
    private int socketTimeout = 30000;
    private int serverAliveInterval = 2000;

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getServerAliveInterval() {
        return serverAliveInterval;
    }

    public void setServerAliveInterval(int serverAliveInterval) {
        this.serverAliveInterval = serverAliveInterval;
    }

    public Map<String, String> toMap() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(KEY_CONNECT_TIMEOUT, String.valueOf(getConnectTimeout()));
        configMap.put(KEY_SOCKET_TIMEOUT, String.valueOf(getSocketTimeout()));
        configMap.put(KEY_SERVER_ALIVE_INTERVAL, String.valueOf(getServerAliveInterval()));
        return configMap;
    }
}