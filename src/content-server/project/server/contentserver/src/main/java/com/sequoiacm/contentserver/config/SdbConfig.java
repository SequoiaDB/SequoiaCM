package com.sequoiacm.contentserver.config;

import com.sequoiacm.common.CommonDefine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.sdb")
public class SdbConfig {
    private int connectTimeout = CommonDefine.DefaultValue.SDB_CONNECT_TIMEOUT;
    private int socketTimeout = CommonDefine.DefaultValue.SDB_SOCKET_TIMEOUT;
    private long maxAutoConnectRetryTime = CommonDefine.DefaultValue.SDB_MAX_CONN_RETRY_TIME;
    private boolean useNagle = CommonDefine.DefaultValue.SDB_USE_NAGLE;
    private boolean useSSL = CommonDefine.DefaultValue.SDB_USE_SSL;
    private int keepAliveTime = CommonDefine.DefaultValue.SDB_KEEP_ALIVE_TIME;
    private int maxConnectionNum = CommonDefine.DefaultValue.SDB_MAX_CONN_NUM;
    private boolean validateConnection = CommonDefine.DefaultValue.SDB_VALIDATE_CONN;
    private int deltaIncCount = CommonDefine.DefaultValue.SDB_DELTA_INC_COUNT;
    private int maxIdleNum = CommonDefine.DefaultValue.SDB_MAX_IDLE_NUM;
    private int recheckCyclePeriod = CommonDefine.DefaultValue.SDB_RECHECK_CYCL_PERIOD;

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
}
