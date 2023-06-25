package com.sequoiacm.infrastructure.config.client.cache.bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.springframework.core.env.Environment;

@ConfigurationProperties("scm.conf.client.bucket")
public class BucketConfCacheConfig {
    private Logger logger = LoggerFactory.getLogger(BucketConfCacheConfig.class);

    private static final int FALLBACK_CACHE_LIMIT = 1000;

    @ScmRewritableConfMarker
    private int cacheLimit = 1000;

    public BucketConfCacheConfig(Environment environment) {
        // 兼容一个老的配置
        String compatibleConf = environment.getProperty("scm.bucket.cacheLimit");
        if (compatibleConf != null) {
            setCacheLimit(Integer.parseInt(compatibleConf));
        }
    }

    public void setCacheLimit(int cacheLimit) {
        if (cacheLimit < 0) {
            logger.warn("Invalid cacheLimit value: {}, set to fallback value: {}.", cacheLimit,
                    FALLBACK_CACHE_LIMIT);
            cacheLimit = FALLBACK_CACHE_LIMIT;
        }
        this.cacheLimit = cacheLimit;
    }

    public int getCacheLimit() {
        return cacheLimit;
    }

}
