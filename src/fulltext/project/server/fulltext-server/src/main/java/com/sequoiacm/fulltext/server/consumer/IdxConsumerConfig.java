package com.sequoiacm.fulltext.server.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("scm.fulltext.consumer")
@Configuration
public class IdxConsumerConfig {
    private static final Logger logger = LoggerFactory.getLogger(IdxConsumerConfig.class);

    // 用于处理消息的线程池配置
    private int corePoolSize = 15;
    private int maxPoolSize = 15;
    private int coreThreadKeepAliveTime = 20; // second
    private int blockingQueueSize = 100;

    // 每次请求消息队列最多返回多少条消息
    private int pullMaxMsgAtOneTime = 14;
    // 最大间隔多长时间请求消息队列获取消息
    private int pullMsgInterval = 200; // ms

    public int getPullMsgInterval() {
        return pullMsgInterval;
    }

    public void setPullMsgInterval(int pullMsgInterval) {
        this.pullMsgInterval = pullMsgInterval;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 2) {
            logger.warn(
                    "scm.fulltext.idxThreadPool.corePoolSize must greater than 2 ({}), reset to 2",
                    corePoolSize);
            this.corePoolSize = 2;
            return;
        }
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getCoreThreadKeepAliveTime() {
        return coreThreadKeepAliveTime;
    }

    public void setCoreThreadKeepAliveTime(int coreThreadKeepAliveTime) {
        this.coreThreadKeepAliveTime = coreThreadKeepAliveTime;
    }

    public int getBlockingQueueSize() {
        return blockingQueueSize;
    }

    public void setBlockingQueueSize(int blockingQueueSize) {
        this.blockingQueueSize = blockingQueueSize;
    }

    public int getPullMaxMsgAtOneTime() {
        return pullMaxMsgAtOneTime;
    }

    public void setPullMaxMsgAtOneTime(int pullMaxMsgAtOneTime) {
        this.pullMaxMsgAtOneTime = pullMaxMsgAtOneTime;
    }
}
