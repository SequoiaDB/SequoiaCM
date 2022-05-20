package com.sequoiacm.infrastructure.lock;

import com.sequoiacm.infrastructure.common.ZkAcl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scm.zookeeper")
public class ScmLockConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScmLockConfig.class);
    private String urls;
    private long cleanJobPeriod = 120L * 1000L;
    private long cleanJobResidualTime = 180L * 1000L;
    private int clenaJobChildThreshold = 1000;
    private int clenaJobCountThreshold = 12 * 60;
    private ZkAcl acl = new ZkAcl();

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
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

    public ZkAcl getAcl() {
        return acl;
    }

    public void setAcl(ZkAcl acl) {
        this.acl = acl;
    }
}