package com.sequoiacm.infrastructure.config.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.config.client")
public class ClientConfig {
    private int subscribeRetryInterval = 5 * 1000;

    public int getSubscribeRetryInterval() {
        return subscribeRetryInterval;
    }

    public void setSubscribeRetryInterval(int subscribeRetryInterval) {
        this.subscribeRetryInterval = subscribeRetryInterval;
    }
}
