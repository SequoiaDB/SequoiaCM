package com.sequoiacm.contentserver.config;

import javax.annotation.PostConstruct;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.sequoiacm.contentserver.job.ScmBackgroundJob;
import com.sequoiacm.contentserver.job.ScmJobManagerRefresher;
import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;

@ConfigurationProperties(prefix = "scm.jobManager.threadpool")
@RefreshScope
public class ScmJobManagerConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScmJobManagerConfig.class);
    private static boolean isInitialized = false;

    private static final int FALLBACK_CORE_SIZE = 8;
    private static final int FALLBACK_MAX_SIZE = 10;
    private static final int FALLBACK_QUEUE_SIZE = 5000;

    /**
     * shortTimeThreadPool
     */
    @ScmRewritableConfMarker
    @ScmRefreshableConfigMarker
    private int coreSize = 8;

    @ScmRewritableConfMarker
    @ScmRefreshableConfigMarker
    private int maxSize = 10;

    @ScmRewritableConfMarker
    private int queueSize = 5000;

    /**
     * @see ScmBackgroundJob#retryOnThreadPoolReject()
     */
    private long defaultTaskWaitingTimeOnReject = 1000 * 10;

    /**
     * longTimeThreadPool
     */
    @ScmRewritableConfMarker
    private int longTimeThreadPoolCoreSize = 10;
    @ScmRewritableConfMarker
    private int longTimeThreadPoolMaxSize = 10;
    @ScmRewritableConfMarker
    private int longTimeThreadPoolQueueSize = 100;
    @ScmRewritableConfMarker
    private int scheduleTaskThreadPoolCoreSize = 10;
    @ScmRewritableConfMarker
    private int scheduleTaskThreadPoolMaxSize = 10;
    @ScmRewritableConfMarker
    private int scheduleTaskThreadPoolQueueSize = 100;

    @PostConstruct
    public void onRefresh() {
        if (coreSize > maxSize) {
            logger.warn("coreSize:{} is greater than maxSize:{}, update coreSize to:{}", coreSize,
                    maxSize, maxSize);
            coreSize = maxSize;
        }
        if (isInitialized) {
            ScmJobManagerRefresher.refreshThreadPoolConfig(coreSize, maxSize);
        }
        else {
            isInitialized = true;
        }
    }

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        if (coreSize < 0) {
            logger.warn("Invalid coreSize value: {}, set to fallback value: {}.", coreSize,
                    FALLBACK_CORE_SIZE);
            coreSize = FALLBACK_CORE_SIZE;
        }
        this.coreSize = coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        if (maxSize <= 0) {
            logger.warn("Invalid maxSize value: {}, set to fallback value: {}.", maxSize,
                    FALLBACK_MAX_SIZE);
            maxSize = FALLBACK_MAX_SIZE;
        }
        this.maxSize = maxSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        if (queueSize <= 0) {
            logger.warn("Invalid queueSize value: {}, set to fallback value: {}.", queueSize,
                    FALLBACK_QUEUE_SIZE);
            queueSize = FALLBACK_QUEUE_SIZE;
        }
        this.queueSize = queueSize;
    }

    public int getLongTimeThreadPoolCoreSize() {
        return longTimeThreadPoolCoreSize;
    }

    public void setLongTimeThreadPoolCoreSize(int longTimeThreadPoolCoreSize) {
        if (longTimeThreadPoolCoreSize < 0) {
            logger.warn("Invalid longTimeThreadPoolCoreSize value: " + longTimeThreadPoolCoreSize
                    + ", set to default value: " + this.longTimeThreadPoolCoreSize);
            return;
        }
        this.longTimeThreadPoolCoreSize = longTimeThreadPoolCoreSize;
    }

    public int getLongTimeThreadPoolMaxSize() {
        return longTimeThreadPoolMaxSize;
    }

    public void setLongTimeThreadPoolMaxSize(int longTimeThreadPoolMaxSize) {
        if (longTimeThreadPoolMaxSize <= 0) {
            logger.warn("Invalid longTimeThreadPoolMaxSize value: " + longTimeThreadPoolMaxSize
                    + ", set to default value: " + this.longTimeThreadPoolMaxSize);
            return;
        }
        this.longTimeThreadPoolMaxSize = longTimeThreadPoolMaxSize;
    }

    public int getLongTimeThreadPoolQueueSize() {
        return longTimeThreadPoolQueueSize;
    }

    public void setLongTimeThreadPoolQueueSize(int longTimeThreadPoolQueueSize) {
        if (longTimeThreadPoolQueueSize <= 0) {
            logger.warn("Invalid longTimeThreadPoolQueueSize value: " + longTimeThreadPoolQueueSize
                    + ", set to default value: " + this.longTimeThreadPoolQueueSize);
            return;
        }
        this.longTimeThreadPoolQueueSize = longTimeThreadPoolQueueSize;
    }

    public int getScheduleTaskThreadPoolCoreSize() {
        return scheduleTaskThreadPoolCoreSize;
    }

    public int getScheduleTaskThreadPoolMaxSize() {
        return scheduleTaskThreadPoolMaxSize;
    }

    public int getScheduleTaskThreadPoolQueueSize() {
        return scheduleTaskThreadPoolQueueSize;
    }

    public void setScheduleTaskThreadPoolQueueSize(int scheduleTaskThreadPoolQueueSize) {
        if (scheduleTaskThreadPoolQueueSize <= 0 || scheduleTaskThreadPoolQueueSize > 100) {
            logger.warn("Invalid scheduleTaskThreadPoolQueueSize value: "
                    + scheduleTaskThreadPoolQueueSize + ", set to default value: "
                    + this.scheduleTaskThreadPoolQueueSize);
            return;
        }
        this.scheduleTaskThreadPoolQueueSize = scheduleTaskThreadPoolQueueSize;
    }

    public long getDefaultTaskWaitingTimeOnReject() {
        return defaultTaskWaitingTimeOnReject;
    }

    public void setDefaultTaskWaitingTimeOnReject(long defaultTaskWaitingTimeOnReject) {
        this.defaultTaskWaitingTimeOnReject = defaultTaskWaitingTimeOnReject;
    }
}
