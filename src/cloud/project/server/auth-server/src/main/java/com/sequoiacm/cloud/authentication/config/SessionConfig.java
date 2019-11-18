package com.sequoiacm.cloud.authentication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scm.session")
public class SessionConfig {
    private int maxInactiveInterval = 1800;
    private int cleanInactiveInterval = 3600;
    private int maxCleanupNum = 500;

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        if (maxInactiveInterval < 60) {
            this.maxInactiveInterval = 60;
        } else {
            this.maxInactiveInterval = maxInactiveInterval;
        }
    }

    public int getCleanInactiveInterval() {
        return cleanInactiveInterval;
    }

    public void setCleanInactiveInterval(int cleanInactiveInterval) {
        if (cleanInactiveInterval > 0 && cleanInactiveInterval < 60) {
            this.cleanInactiveInterval = 60;
        } else if (cleanInactiveInterval <= 0) {
            this.cleanInactiveInterval = 0;
        } else {
            this.cleanInactiveInterval = cleanInactiveInterval;
        }
    }

    public int getMaxCleanupNum() {
        return maxCleanupNum;
    }

    public void setMaxCleanupNum(int maxCleanupNum) {
        if (maxCleanupNum > 0) {
            this.maxCleanupNum = maxCleanupNum;
        }
    }
}
