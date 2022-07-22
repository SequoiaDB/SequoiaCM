package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.context")
public class ContextConfig {
    private int keepaliveTime = 60 * 1000 * 60;
    private int cleanPeriod = 60 * 1000 * 30;

    public int getCleanPeriod() {
        return cleanPeriod;
    }

    public void setCleanPeriod(int cleanPeriod) {
        this.cleanPeriod = cleanPeriod;
    }

    public int getKeepaliveTime() {
        return keepaliveTime;
    }

    public void setKeepaliveTime(int keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
    }
}
