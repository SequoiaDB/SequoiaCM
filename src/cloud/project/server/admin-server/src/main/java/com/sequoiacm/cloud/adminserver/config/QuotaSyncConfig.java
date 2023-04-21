package com.sequoiacm.cloud.adminserver.config;

import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "scm.quota.sync")
public class QuotaSyncConfig {

    @ScmRefreshableConfigMarker
    private long maxTimeDiff = 30 * 1000; // 30s（单位 ms）

    @ScmRefreshableConfigMarker
    private int maxConcurrentCount = 30;

    @ScmRefreshableConfigMarker
    private int retryInterval = 300; // 300s（单位 s）

    @ScmRefreshableConfigMarker
    private int expireTime = 5 * 60; // 5min（单位 s）

    public long getMaxTimeDiff() {
        return maxTimeDiff;
    }

    public void setMaxTimeDiff(long maxTimeDiff) {
        checkGreaterThanZero(maxTimeDiff, "maxTimeDiff");
        this.maxTimeDiff = maxTimeDiff;
    }

    public int getMaxConcurrentCount() {
        return maxConcurrentCount;
    }

    public void setMaxConcurrentCount(int maxConcurrentCount) {
        checkGreaterThanZero(maxConcurrentCount, "maxConcurrentCount");
        this.maxConcurrentCount = maxConcurrentCount;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        checkGreaterThanZero(retryInterval, "retryInterval");
        this.retryInterval = retryInterval;
    }

    public int getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(int expireTime) {
        checkGreaterThanZero(expireTime, "expireTime");
        this.expireTime = expireTime;
    }

    private void checkGreaterThanZero(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
    }

    @Override
    public String toString() {
        return "QuotaSyncConfig{" + "maxTimeDiff=" + maxTimeDiff + ", maxConcurrentCount="
                + maxConcurrentCount + ", retryInterval=" + retryInterval + ", expireTime="
                + expireTime + '}';
    }
}
