package com.sequoiacm.mq.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scm.mq.consumer")
public class ConsumerConfProperties {
    private long pullMsgInterval = 500;
    private long acquirePartitionInterval = 5000;

    public long getAcquirePartitionInterval() {
        return acquirePartitionInterval;
    }

    public void setAcquirePartitionInterval(long acquirePartitionInterval) {
        this.acquirePartitionInterval = acquirePartitionInterval;
    }

    public long getPullMsgInterval() {
        return pullMsgInterval;
    }

    public void setPullMsgInterval(long pullMsgInterval) {
        this.pullMsgInterval = pullMsgInterval;
    }
}
