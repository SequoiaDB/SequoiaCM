package com.sequoiacm.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scm.request.keepAlive")
public class RequestKeepAliveConfig {
    private static final Logger logger = LoggerFactory.getLogger(RequestKeepAliveConfig.class);

    private long interval = 15 * 1000;

    public RequestKeepAliveConfig(
            @Value("${ribbon.ReadTimeout:30000}") long ribbonReadTimeout) {
        long interval = ribbonReadTimeout / 2;
        if (interval > 0) {
            setInterval(interval);
        }
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException(
                    "scm.request.keepAlive.interval must greater than 0, value:" + interval);
        }
        this.interval = interval;
        logger.info("scm.request.keepAlive.interval value:{}ms", this.interval);
    }
}
