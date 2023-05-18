package com.sequoiacm.infrastructure.config.client.core.bucket;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scm.conf.client.bucket")
public class BucketSubscriberConfig {
    private Logger logger = LoggerFactory.getLogger(BucketSubscriberConfig.class);
    @ScmRewritableConfMarker
    private int cacheLimit = 1000;

    @ScmRewritableConfMarker
    private long heartbeatInterval = 3 * 60 * 1000; // 3 min

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
        if (heartbeatInterval <= 0) {
            logger.warn("Invalid interval value of bucket heartbeat: " + heartbeatInterval
                    + ", set to default value: " + this.heartbeatInterval);
            return;
        }
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
}
