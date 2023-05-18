package com.sequoiacm.contentserver.config;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.cacheStrategy.auto")
public class AutoConfig {
    private static final Logger logger = LoggerFactory.getLogger(AutoConfig.class);
    @ScmRewritableConfMarker
    private int days = 3;

    private int accessCount = 3;

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        if (days > 0) {
            this.days = days;
            return;
        }
        logger.warn("invalid scm.cacheStrategy.auto.days value, correct to old value:" + this.days);
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(int accessCount) {
        if (accessCount > 0 && accessCount <= 10) {
            this.accessCount = accessCount;
            return;
        }
        logger.warn("invalid scm.cacheStrategy.auto.accessCount value, correct to old value:"
                + this.accessCount);
    }
}
