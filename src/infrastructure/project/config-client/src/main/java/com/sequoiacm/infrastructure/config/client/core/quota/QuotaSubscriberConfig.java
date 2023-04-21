package com.sequoiacm.infrastructure.config.client.core.quota;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scm.conf.client.quota")
public class QuotaSubscriberConfig {

    private long heartbeatInterval = 180000;

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
}
