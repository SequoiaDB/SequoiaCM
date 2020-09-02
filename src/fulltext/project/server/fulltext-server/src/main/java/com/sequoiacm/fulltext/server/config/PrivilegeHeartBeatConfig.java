package com.sequoiacm.fulltext.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.privilege.heartbeat")
public class PrivilegeHeartBeatConfig {
    private static final Logger logger = LoggerFactory.getLogger(PrivilegeHeartBeatConfig.class);

    private int interval = 10 * 1000;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        if (interval <= 0) {
            logger.warn("Invalid interval value: " + interval + ", set to default value: "
                    + this.interval);
            return;
        }
        this.interval = interval;
    }
}
