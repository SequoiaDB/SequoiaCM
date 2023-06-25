package com.sequoiacm.cloud.adminserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;

@Component
@ConfigurationProperties(prefix = "scm.privilege.heartbeat")
public class PrivilegeHeartBeatConfig {
    private static final Logger logger = LoggerFactory.getLogger(PrivilegeHeartBeatConfig.class);

    private static final int FALLBACK_INTERVAL = 10 * 1000;

    @ScmRewritableConfMarker
    private int interval = 10 * 1000;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        if (interval <= 0) {
            logger.warn("Invalid interval value: {}, set to fallback value: {}.", interval,
                    FALLBACK_INTERVAL);
            interval = FALLBACK_INTERVAL;
        }
        this.interval = interval;
    }
}
