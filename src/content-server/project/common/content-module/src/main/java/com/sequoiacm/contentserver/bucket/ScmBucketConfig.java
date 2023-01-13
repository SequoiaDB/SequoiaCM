package com.sequoiacm.contentserver.bucket;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scm.bucket")
public class ScmBucketConfig {
    private Logger logger = LoggerFactory.getLogger(ScmBucketConfig.class);
    @ScmRewritableConfMarker
    private int cacheLimit = 1000;

    public int getCacheLimit() {
        return cacheLimit;
    }

    public void setCacheLimit(int cacheLimit) {
        if (cacheLimit < 0) {
            logger.warn("invalid bucket cache limit:{}, reset to default value:{}", cacheLimit,
                    this.cacheLimit);
            return;
        }
        this.cacheLimit = cacheLimit;
    }
}
