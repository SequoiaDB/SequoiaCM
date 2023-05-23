package com.sequoiacm.infrastructure.config.client.core.user;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scm.conf.client.user")
public class UserConfSubscriberConfig {

    private static final Logger logger = LoggerFactory.getLogger(UserConfSubscriberConfig.class);

    @ScmRewritableConfMarker
    private long heartbeatInterval = 3 * 60 * 1000; // 3 min

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        if (heartbeatInterval <= 0) {
            logger.warn("Invalid interval value of user heartbeat: " + heartbeatInterval
                    + ", set to default value: " + this.heartbeatInterval);
            return;
        }
        this.heartbeatInterval = heartbeatInterval;
    }
}
