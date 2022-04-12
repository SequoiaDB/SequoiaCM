package com.sequoiacm.contentserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;

@Component
@ConfigurationProperties(prefix = "scm.zookeeper")
public class ZkConfig {
    private static final Logger logger = LoggerFactory.getLogger(ZkConfig.class);

    private String urls = CommonDefine.DefaultValue.ZK_URL;
    private int lockTimeout = CommonDefine.DefaultValue.ZK_LOCK_TIMEOUT;
    private int clientTimeout = CommonDefine.DefaultValue.ZK_CLIENT_TIMEOUT;
    private long cleanJobPeriod = CommonDefine.DefaultValue.ZK_CLEANJOB_PERIOD;
    private long cleanJobResidualTime = CommonDefine.DefaultValue.ZK_CLEANJOB_RESIDUAL;
    private int clenaJobChildThreshold = CommonDefine.DefaultValue.ZK_CLEANJOB_CHILDNUM_THRESHOLD;
    private int clenaJobCountThreshold = CommonDefine.DefaultValue.ZK_CLEANJOB_COUNT_THRESHOLD;

    public long getCleanJobPeriod() {
        return cleanJobPeriod;
    }

    public void setCleanJobPeriod(long cleanJobPeriod) {
        if (cleanJobPeriod <= 1000) {
            logger.warn("Invalid cleanJobPeriod value: " + cleanJobPeriod
                    + ", set to default value: " + this.cleanJobPeriod);
            return;
        }

        this.cleanJobPeriod = cleanJobPeriod;
    }

    public long getCleanJobResidualTime() {
        return cleanJobResidualTime;
    }

    public void setCleanJobResidualTime(long cleanJobResidualTime) {
        if (cleanJobResidualTime <= 1000) {
            logger.warn("Invalid cleanJobResidualTime value: " + cleanJobResidualTime
                    + ", set to default value: " + this.cleanJobResidualTime);
            return;
        }

        this.cleanJobResidualTime = cleanJobResidualTime;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(int clientTimeout) {
        if (clientTimeout <= 0) {
            logger.warn("Invalid client timeout value: " + clientTimeout
                    + ", set to default value: " + this.clientTimeout);
            return;
        }
        this.clientTimeout = clientTimeout;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public int getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(int lockTimeout) {
        if (lockTimeout <= 0) {
            logger.warn("Invalid lock timeout value: " + lockTimeout + ", set to default value: "
                    + this.lockTimeout);
            return;
        }
        this.lockTimeout = lockTimeout;
    }

    public int getClenaJobChildThreshold() {
        return clenaJobChildThreshold;
    }

    public void setClenaJobChildThreshold(int clenaJobChildThreshold) {
        if (clenaJobChildThreshold < 10 || clenaJobChildThreshold > 10000) {
            logger.warn("Invalid clenaJobChildThreshold value: " + clenaJobChildThreshold
                    + ", set to default value: " + this.clenaJobChildThreshold);
            return;
        }
        this.clenaJobChildThreshold = clenaJobChildThreshold;
    }

    public int getClenaJobCountThreshold() {
        return clenaJobCountThreshold;
    }

    public void setClenaJobCountThreshold(int clenaJobCountThreshold) {
        if (clenaJobCountThreshold <= 0) {
            logger.warn("Invalid clenaJobCountThreshold value: " + clenaJobCountThreshold
                    + ", set to default value: " + this.clenaJobCountThreshold);
            return;
        }
        this.clenaJobCountThreshold = clenaJobCountThreshold;
    }
}
