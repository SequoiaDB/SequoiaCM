package com.sequoiacm.infrastructure.config.client.core.quota;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scm.conf.client.quota")
public class QuotaSubscriberConfig {

    private Logger logger = LoggerFactory.getLogger(QuotaSubscriberConfig.class);

    @ScmRewritableConfMarker
    private long heartbeatInterval = 3 * 60 * 1000; // 3 min

    public void setHeartbeatInterval(long heartbeatInterval) {
        if (heartbeatInterval <= 0) {
            logger.warn("Invalid interval value of quota heartbeat: " + heartbeatInterval
                    + ", set to default value: " + this.heartbeatInterval);
            return;
        }
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
}
