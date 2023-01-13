package com.sequoiacm.fulltext.server.sch;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("scm.fulltext.idxThreadPool")
@Configuration
public class IdxThreadPoolConfig {
    private static final Logger logger = LoggerFactory.getLogger(IdxThreadPoolConfig.class);
    @ScmRewritableConfMarker
    private int corePoolSize = 5;
    private int maxPoolSize = 10;
    private int keepAliveTime = 20; //second
    private int blockingQueueSize = 100;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        if(corePoolSize < 2){
            logger.warn("scm.fulltext.idxThreadPool.corePoolSize must greater than 2 ({}), reset to 2", corePoolSize);
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

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getBlockingQueueSize() {
        return blockingQueueSize;
    }

    public void setBlockingQueueSize(int blockingQueueSize) {
        this.blockingQueueSize = blockingQueueSize;
    }

}