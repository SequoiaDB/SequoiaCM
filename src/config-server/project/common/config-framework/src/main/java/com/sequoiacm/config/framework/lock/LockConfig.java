package com.sequoiacm.config.framework.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.zookeeper")
public class LockConfig {
    private static final Logger logger = LoggerFactory.getLogger(LockConfig.class);
    private String urls;
    private int clientNum = 10;
    private long cleanJobPeriod = 120L * 1000L;
    private long cleanJobResidualTime = 180L * 1000L;
    private int clenaJobChildThreshold = 1000;
    private int clenaJobCountThreshold = 12 * 60;

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public int getClientNum() {
        return clientNum;
    }

    public void setClientNum(int clientNum) {
        if (clientNum <= 0) {
            logger.warn("Invalid client num value: " + clientNum + ", set to default value: "
                    + this.clientNum);
            return;
        }
        this.clientNum = clientNum;
    }

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
