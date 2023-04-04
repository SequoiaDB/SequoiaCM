package com.sequoiacm.infrastructure.config.client.core.role;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scm.conf.version.role")
public class RoleConfSubscriberConfig {

    private long heartbeatInterval = 180000;

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}
