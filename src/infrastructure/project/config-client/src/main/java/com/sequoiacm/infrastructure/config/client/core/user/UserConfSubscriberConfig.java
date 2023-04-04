package com.sequoiacm.infrastructure.config.client.core.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scm.conf.version.user")
public class UserConfSubscriberConfig {

    private long heartbeatInterval = 180000;

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}
