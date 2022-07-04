package com.sequoiacm.infrastructure.config.client.core.bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scm.conf.client.bucket")
public class BucketSubscriberConfig {
    private Logger logger = LoggerFactory.getLogger(BucketSubscriberConfig.class);
    private int cacheLimit = 1000;
    private long heartbeatInterval = 180000;

    public void setCacheLimit(int cacheLimit) {
        if (cacheLimit < 0) {
            logger.warn("invalid bucket cache limit:{}, reset to default value:{}", cacheLimit,
                    this.cacheLimit);
            return;
        }
        this.cacheLimit = cacheLimit;
    }

    public int getCacheLimit() {
        return cacheLimit;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
}
