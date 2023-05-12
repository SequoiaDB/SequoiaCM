package com.sequoiacm.contentserver.config;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.PropertiesDefine;
import org.springframework.beans.factory.annotation.Value;
import com.sequoiadb.datasource.ConnectStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.store.sequoiadb")
public class SdbConfig {
    @Value("${" + PropertiesDefine.PROPERTY_SDB_CONNECTTIMEOUT + ":10000}")
    private int connectTimeout = CommonDefine.DefaultValue.SDB_CONNECT_TIMEOUT;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_SOCKETTIMEOUT + ":0}")
    private int socketTimeout = CommonDefine.DefaultValue.SDB_SOCKET_TIMEOUT;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_MAXCONNECTRETRYTIME + ":15000}")
    private long maxAutoConnectRetryTime = CommonDefine.DefaultValue.SDB_MAX_CONN_RETRY_TIME;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_USENAGLE + ":false}")
    private boolean useNagle = CommonDefine.DefaultValue.SDB_USE_NAGLE;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_USESSL + ":false}")
    private boolean useSSL = CommonDefine.DefaultValue.SDB_USE_SSL;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_KEEPALIVETIME + ":60000}")
    private int keepAliveTime = CommonDefine.DefaultValue.SDB_KEEP_ALIVE_TIME;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_MAXCONNECTIONNUM + ":500}")
    private int maxConnectionNum = CommonDefine.DefaultValue.SDB_MAX_CONN_NUM;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_VALIDATECONNECTION + ":true}")
    private boolean validateConnection = CommonDefine.DefaultValue.SDB_VALIDATE_CONN;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_DELTAINCCOUNT + ":10}")
    private int deltaIncCount = CommonDefine.DefaultValue.SDB_DELTA_INC_COUNT;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_MAXIDLENUM + ":10}")
    private int maxIdleNum = CommonDefine.DefaultValue.SDB_MAX_IDLE_NUM;

    @Value("${" + PropertiesDefine.PROPERTY_SDB_RECHECKPERIOD + ":30000}")
    private int recheckCyclePeriod = CommonDefine.DefaultValue.SDB_RECHECK_CYCL_PERIOD;
    @Value("${" + PropertiesDefine.PROPERTY_SDB_CONNECTSTRATEGY + ":SERIAL}")
    private String connectStrategy = CommonDefine.DefaultValue.SDB_CONNECT_STRATEGY.toString();
    private String location;

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

    public ConnectStrategy getConnectStrategy() {
        return ConnectStrategy.valueOf(connectStrategy);
    }

    public void setConnectStrategy(ConnectStrategy connectStrategy) {
        this.connectStrategy = connectStrategy.toString();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
