package com.sequoiacm.mq.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.mq.msgCleaner")
public class MsgCleanerConfig {
    private Logger logger = LoggerFactory.getLogger(MsgCleanerConfig.class);
    private long period = 5 * 60 * 1000;
    private long msgCountThreshold = 20 * 10000;

    public long getPeriod() {
        return period;
    }

    public long getMsgCountThreshold() {
        return msgCountThreshold;
    }

    public void setMsgCountThreshold(long msgCountThreshold) {
        if (msgCountThreshold < 0) {
            logger.warn("scm.mq.msgCleaner.msgCountThreshold is not valid:" + msgCountThreshold
                    + ", reset to 1000");
            this.msgCountThreshold = 1000;
        }
        this.msgCountThreshold = msgCountThreshold;
    }

    public void setPeriod(long period) {
        if (period <= 1000) {
            logger.warn("scm.mq.msgCleaner.period is not valid:" + period + ", reset to 1000");
            this.period = 1000;
        }
        this.period = period;
    }
}
