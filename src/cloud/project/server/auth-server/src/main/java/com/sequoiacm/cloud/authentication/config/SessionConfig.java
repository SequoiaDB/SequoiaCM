package com.sequoiacm.cloud.authentication.config;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scm.session")
public class SessionConfig {

    private static final int FALLBACK_MAX_INACTIVE_INTERVAL_LOWER_BOUND = 60;

    private static final int FALLBACK_CLEAN_INACTIVE_INTERVAL_LOWER_BOUND = 60;
    private static final int FALLBACK_CLEAN_INACTIVE_INTERVAL_ZERO = 0;

    private static final int FALLBACK_MAX_CLEANUP_NUM = 500;

    @ScmRewritableConfMarker
    private int maxInactiveInterval = 1800;
    @ScmRewritableConfMarker
    private int cleanInactiveInterval = 3600;
    @ScmRewritableConfMarker
    private int maxCleanupNum = 500;

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        if (maxInactiveInterval < 60) {
            this.maxInactiveInterval = FALLBACK_MAX_INACTIVE_INTERVAL_LOWER_BOUND;
        } else {
            this.maxInactiveInterval = maxInactiveInterval;
        }
    }

    public int getCleanInactiveInterval() {
        return cleanInactiveInterval;
    }

    public void setCleanInactiveInterval(int cleanInactiveInterval) {
        if (cleanInactiveInterval > 0 && cleanInactiveInterval < 60) {
            this.cleanInactiveInterval = FALLBACK_CLEAN_INACTIVE_INTERVAL_LOWER_BOUND;
        } else if (cleanInactiveInterval <= 0) {
            this.cleanInactiveInterval = FALLBACK_CLEAN_INACTIVE_INTERVAL_ZERO;
        } else {
            this.cleanInactiveInterval = cleanInactiveInterval;
        }
    }

    public int getMaxCleanupNum() {
        return maxCleanupNum;
    }

    public void setMaxCleanupNum(int maxCleanupNum) {
        if (maxCleanupNum <= 0) {
            maxCleanupNum = FALLBACK_MAX_CLEANUP_NUM;
        }
        this.maxCleanupNum = maxCleanupNum;
    }
}
