package com.sequoiacm.mq.server.config;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.mq.consumerChecker")
public class ConsumerHealthCheckerConfig {
    private Logger logger = LoggerFactory.getLogger(ConsumerHealthCheckerConfig.class);
    @ScmRewritableConfMarker
    private long period = 1 * 60 * 1000;
    @ScmRewritableConfMarker
    private long idleThreshold = 10 * 1000;

    public long getPeriod() {
        return period;
    }

    public long getIdleThreshold() {
        return idleThreshold;
    }

    public void setIdleThreshold(long threshold) {
        if (threshold <= 0) {
            logger.warn("scm.mq.consumerChecker.idleThreshold is not valid:" + threshold
                    + ", reset to 1000");
            this.idleThreshold = 1000;
            return;
        }
        this.idleThreshold = threshold;
    }

    public void setPeriod(long period) {
        if (period <= 1000) {
            logger.warn("scm.mq.consumerChecker.period is not valid:" + period + ", reset to 1000");
            this.period = 1000;
            return;
        }
        this.period = period;
    }
}
