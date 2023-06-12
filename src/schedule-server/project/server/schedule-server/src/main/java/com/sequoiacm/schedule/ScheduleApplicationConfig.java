package com.sequoiacm.schedule;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.datasource.DatasourceOptions;

@Component
@ConfigurationProperties(prefix = "scm.store.sequoiadb")
class ConfigSdb {
    private String urls;
    private String username;
    private String password;

    private final ConfigOptions connConf = new ConfigOptions();
    private final DatasourceOptions dsConf = new DatasourceOptions();
    private int connectTimeout = connConf.getConnectTimeout();
    private int socketTimeout = connConf.getSocketTimeout();
    private long maxAutoConnectRetryTime = connConf.getMaxAutoConnectRetryTime();
    private boolean useNagle = connConf.getUseNagle();
    private boolean useSSL = connConf.getUseSSL();
    private int keepAliveTime = 60 * 1000;
    private int maxConnectionNum = dsConf.getMaxCount();
    private boolean validateConnection = true;
    private int deltaIncCount = dsConf.getDeltaIncCount();
    private int maxIdleNum = 2;
    private int recheckCyclePeriod = 30 * 1000;
    private String location;

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

@Component
@ConfigurationProperties(prefix = "scm.zookeeper")
class ConfigZookeeper {
    private String urls;
    private int maxClientCnxns;
    private int sessionTimeout;
    private int nodeLifeCycle;
    private int clearNodePeriod;
    private ZkAcl acl = new ZkAcl();

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public int getMaxClientCnxns() {
        return maxClientCnxns;
    }

    public void setMaxClientCnxns(int maxClientCnxns) {
        this.maxClientCnxns = maxClientCnxns;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public int getNodeLifeCycle() {
        return nodeLifeCycle;
    }

    public void setNodeLifeCycle(int nodeLifeCycle) {
        this.nodeLifeCycle = nodeLifeCycle;
    }

    public int getClearNodePeriod() {
        return clearNodePeriod;
    }

    public ZkAcl getAcl() {
        return acl;
    }

    public void setAcl(ZkAcl acl) {
        this.acl = acl;
    }
}

@Component
@ConfigurationProperties(prefix = "scm.revote")
class ConfigRevoteInterval {
    private long initialInterval = 100;
    private double intervalMultiplier = 2;
    private long maxInterval = 60000;

    public long getInitialInterval() {
        return initialInterval;
    }

    public void setInitialInterval(long initialInterval) {
        this.initialInterval = initialInterval;
    }

    public double getIntervalMultiplier() {
        return intervalMultiplier;
    }

    public void setIntervalMultiplier(double intervalMultiplier) {
        this.intervalMultiplier = intervalMultiplier;
    }

    public long getMaxInterval() {
        return maxInterval;
    }

    public void setMaxInterval(long maxInterval) {
        this.maxInterval = maxInterval;
    }
}

@Component
@ConfigurationProperties(prefix = "scm.internalSchedule")
class ConfigInternalSchedule {
    private static final Logger logger = LoggerFactory.getLogger(ConfigInternalSchedule.class);
    //second
    @ScmRewritableConfMarker
    private int healthCheckInterval = 30;

    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(int healthCheckInterval) {
        if (healthCheckInterval < 1) {
            logger.warn(
                    "invalid value for scm.internalSchedule.healthCheckInterval:{}, reset to 30s",
                    healthCheckInterval);
            this.healthCheckInterval = 30;
            return;
        }
        this.healthCheckInterval = healthCheckInterval;
    }

}

@Component
@Configuration
public class ScheduleApplicationConfig {
    @Autowired
    ConfigZookeeper zooConfig;

    @Autowired
    private ConfigSdb configSdb;
    @Autowired
    private ConfigInternalSchedule configInternalSch;
    @Autowired
    private ConfigRevoteInterval configRevoteInterval;

    @Value("${server.port}")
    String serverPort;

    @Value("${scm.privilege.heartbeat.interval:10000}")
    private String heartbeatInterval;

    public int getConnectTimeout() {
        return configSdb.getConnectTimeout();
    }

    public int getSocketTimeout() {
        return configSdb.getSocketTimeout();
    }

    public long getMaxAutoConnectRetryTime() {
        return configSdb.getMaxAutoConnectRetryTime();
    }

    public boolean getUseNagle() {
        return configSdb.getUseNagle();
    }

    public boolean getUseSSL() {
        return configSdb.getUseSSL();
    }

    public int getKeepAliveTime() {
        return configSdb.getKeepAliveTime();
    }

    public int getMaxConnectionNum() {
        return configSdb.getMaxConnectionNum();
    }

    public boolean getValidateConnection() {
        return configSdb.getValidateConnection();
    }

    public int getDeltaIncCount() {
        return configSdb.getDeltaIncCount();
    }

    public int getMaxIdleNum() {
        return configSdb.getMaxIdleNum();
    }

    public int getRecheckCyclePeriod() {
        return configSdb.getRecheckCyclePeriod();
    }

    public String getLocation() {
        return configSdb.getLocation();
    }

    public ConfigSdb getConfigSdb() {
        return configSdb;
    }

    public String getZookeeperUrl() {
        return zooConfig.getUrls();
    }

    public ZkAcl getZookeeperAcl(){
        return zooConfig.getAcl();
    }

    public String getMetaUrl() {
        return configSdb.getUrls();
    }

    public String getMetaUser() {
        return configSdb.getUsername();
    }

    public String getMetaPassword() {
        return configSdb.getPassword();
    }

    public String getServerPort() {
        return serverPort;
    }

    public long getPriHBInterval() {
        return Long.parseLong(heartbeatInterval);
    }

    public long getRevoteInitialInterval() {
        return configRevoteInterval.getInitialInterval();
    }

    public long getRevoteMaxInterval() {
        return configRevoteInterval.getMaxInterval();
    }

    public double getRevoteIntervalMultiplier() {
        return configRevoteInterval.getIntervalMultiplier();
    }

    public int getInternalSchHealthCheckInterval() {
        return configInternalSch.getHealthCheckInterval();
    }
}
