package com.sequoiacm.contentserver.config;

import com.sequoiacm.infrastructure.common.ScmQuotaUtils;
import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;
import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.quota")
@RefreshScope
public class QuotaLimitConfig {
    private static final Logger logger = LoggerFactory.getLogger(QuotaLimitConfig.class);

    public static final String POLICY_SYNC = "sync";
    public static final String POLICY_ASYNC = "async";

    @ScmRefreshableConfigMarker
    private LowWater lowWater = new LowWater();

    @ScmRefreshableConfigMarker
    private HighWater highWater = new HighWater();

    @ScmRefreshableConfigMarker
    private AsyncStrategy asyncStrategy = new AsyncStrategy();

    @ScmRewritableConfMarker
    @ScmRefreshableConfigMarker
    private long refreshSyncInfoInterval = 10 * 1000;

    @ScmRefreshableConfigMarker
    private int maxStatisticsThreads = 30;

    public LowWater getLowWater() {
        return lowWater;
    }

    public void setLowWater(LowWater lowWater) {
        this.lowWater = lowWater;
    }

    public HighWater getHighWater() {
        return highWater;
    }

    public void setHighWater(HighWater highWater) {
        this.highWater = highWater;
    }

    public long getRefreshSyncInfoInterval() {
        return refreshSyncInfoInterval;
    }

    public void setRefreshSyncInfoInterval(long refreshSyncInfoInterval) {
        if (refreshSyncInfoInterval < 1000) {
            refreshSyncInfoInterval = 1000;
            logger.warn("refreshStatisticsInterval is too small, set to 1000ms");
        }
        this.refreshSyncInfoInterval = refreshSyncInfoInterval;
    }

    public int getMaxStatisticsThreads() {
        return maxStatisticsThreads;
    }

    public void setMaxStatisticsThreads(int maxStatisticsThreads) {
        checkGraterThanZero(maxStatisticsThreads, "maxStatisticsThreads");
        this.maxStatisticsThreads = maxStatisticsThreads;
    }

    private static void checkGraterThanZero(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
    }

    public AsyncStrategy getAsyncStrategy() {
        return asyncStrategy;
    }

    public void setAsyncStrategy(AsyncStrategy asyncStrategy) {
        this.asyncStrategy = asyncStrategy;
    }

    public class LowWater {

        @ScmRefreshableConfigMarker
        private int rate = 95;

        @ScmRefreshableConfigMarker
        private int minObjects = 500;

        @ScmRefreshableConfigMarker
        private String minSize = "1g";

        @ScmRefreshableConfigMarker
        private String policy = "async";

        private long minSizeBytes = ScmQuotaUtils.convertToBytes(minSize);

        public int getRate() {
            return rate;
        }

        public void setRate(int rate) {
            if (rate < 0 || rate > 100) {
                throw new IllegalArgumentException("rate must be between 0 and 100");
            }
            this.rate = rate;
        }

        public int getMinObjects() {
            return minObjects;
        }

        public void setMinObjects(int minObjects) {
            checkGraterThanZero(minObjects, "minObjects");
            this.minObjects = minObjects;
        }

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            if (!POLICY_SYNC.equals(policy) && !POLICY_ASYNC.equals(policy)) {
                throw new IllegalArgumentException("policy must be sync or async:policy=" + policy);
            }
            this.policy = policy;
        }

        public String getMinSize() {
            return minSize;
        }

        public void setMinSize(String minSize) {
            if (minSize == null || minSize.isEmpty()) {
                throw new IllegalArgumentException("minSize can not be null or empty");
            }
            long bytes = ScmQuotaUtils.convertToBytes(minSize);
            if (bytes <= 0) {
                throw new IllegalArgumentException(
                        "minSize must be greater than 0:minSize=" + minSize);
            }
            this.minSize = minSize;
            this.minSizeBytes = bytes;
        }

        public long getMinSizeBytes() {
            return minSizeBytes;
        }

    }

    public static class HighWater {

        @ScmRefreshableConfigMarker
        private String policy = "sync";

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            if (!POLICY_SYNC.equals(policy) && !POLICY_ASYNC.equals(policy)) {
                throw new IllegalArgumentException("policy must be sync or async:policy=" + policy);
            }
            this.policy = policy;
        }
    }

    public static class AsyncStrategy {

        @ScmRewritableConfMarker
        @ScmRefreshableConfigMarker
        private long flushInterval = 1000 * 10; // 10s

        @ScmRefreshableConfigMarker
        private int maxCacheObjects = 500;

        @ScmRefreshableConfigMarker
        private String maxCacheSize = "1g";
        private long maxCacheSizeBytes = ScmQuotaUtils.convertToBytes(maxCacheSize);

        public long getFlushInterval() {
            return flushInterval;
        }

        public void setFlushInterval(long flushInterval) {
            if (flushInterval < 1000) {
                flushInterval = 1000;
                logger.warn("flushInterval is too small, set to 1000ms");
            }
            this.flushInterval = flushInterval;
        }

        public int getMaxCacheObjects() {
            return maxCacheObjects;
        }

        public void setMaxCacheObjects(int maxCacheObjects) {
            checkGraterThanZero(maxCacheObjects, "maxCacheObjects");
            this.maxCacheObjects = maxCacheObjects;
        }

        public String getMaxCacheSize() {
            return maxCacheSize;
        }

        public void setMaxCacheSize(String maxCacheSize) {
            if (maxCacheSize == null || maxCacheSize.isEmpty()) {
                throw new IllegalArgumentException("maxCacheSize can not be null or empty");
            }
            long bytes = ScmQuotaUtils.convertToBytes(maxCacheSize);
            if (bytes <= 0) {
                throw new IllegalArgumentException(
                        "maxCacheSize must be greater than 0:maxCacheSize=" + maxCacheSize);
            }
            this.maxCacheSizeBytes = bytes;
            this.maxCacheSize = maxCacheSize;
        }

        public long getMaxCacheSizeBytes() {
            return maxCacheSizeBytes;
        }

    }

}
