package com.sequoiacm.schedule.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scm.schedule.worker")
public class ScheduleWorkerConfig {
    private int threadPoolSize = 10;
    private int threadPoolPendingQueueSize = 50;

    public void setThreadPoolPendingQueueSize(int threadPoolPendingQueueSize) {
        this.threadPoolPendingQueueSize = threadPoolPendingQueueSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public int getThreadPoolPendingQueueSize() {
        return threadPoolPendingQueueSize;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }
}
