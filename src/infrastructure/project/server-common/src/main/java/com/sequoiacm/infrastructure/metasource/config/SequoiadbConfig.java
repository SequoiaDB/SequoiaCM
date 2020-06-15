package com.sequoiacm.infrastructure.metasource.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.datasource.DatasourceOptions;

@Configuration
@ConfigurationProperties(prefix = "scm.store.sequoiadb")
public class SequoiadbConfig {
    private List<String> urls;
    private String username;
    private String password;

    private final ConfigOptions connConf = new ConfigOptions();
    private final DatasourceOptions dsConf = new DatasourceOptions();
    private int connectTimeout = connConf.getConnectTimeout();
    private int socketTimeout = connConf.getSocketTimeout();
    private long maxAutoConnectRetryTime = connConf.getMaxAutoConnectRetryTime();
    private boolean useNagle = connConf.getUseNagle();
    private boolean useSSL = connConf.getUseSSL();
    private int keepAliveTime = dsConf.getKeepAliveTimeout();
    private int maxConnectionNum = dsConf.getMaxCount();
    private boolean validateConnection = true;
    private int deltaIncCount = dsConf.getDeltaIncCount();
    private int maxIdleNum = dsConf.getMaxIdleCount();
    private int recheckCyclePeriod = dsConf.getCheckInterval();

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

    public long getMaxAutoConnectRetryTime() {
        return maxAutoConnectRetryTime;
    }

    public void setMaxAutoConnectRetryTime(long maxAutoConnectRetryTime) {
        this.maxAutoConnectRetryTime = maxAutoConnectRetryTime;
    }

    public boolean getUseNagle() {
        return useNagle;
    }

    public void setUseNagle(boolean useNagle) {
        this.useNagle = useNagle;
    }

    public boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getMaxConnectionNum() {
        return maxConnectionNum;
    }

    public void setMaxConnectionNum(int maxConnectionNum) {
        this.maxConnectionNum = maxConnectionNum;
    }

    public boolean getValidateConnection() {
        return validateConnection;
    }

    public void setValidateConnection(boolean validateConnection) {
        this.validateConnection = validateConnection;
    }

    public int getDeltaIncCount() {
        return deltaIncCount;
    }

    public void setDeltaIncCount(int deltaIncCount) {
        this.deltaIncCount = deltaIncCount;
    }

    public int getMaxIdleNum() {
        return maxIdleNum;
    }

    public void setMaxIdleNum(int maxIdleNum) {
        this.maxIdleNum = maxIdleNum;
    }

    public int getRecheckCyclePeriod() {
        return recheckCyclePeriod;
    }

    public void setRecheckCyclePeriod(int recheckCyclePeriod) {
        this.recheckCyclePeriod = recheckCyclePeriod;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
