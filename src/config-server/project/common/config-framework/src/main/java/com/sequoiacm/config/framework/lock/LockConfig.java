package com.sequoiacm.config.framework.lock;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "scm.zookeeper")
public class LockConfig {
    private static final Logger logger = LoggerFactory.getLogger(LockConfig.class);

    private static final long FALLBACK_CLEAN_JOB_PERIOD = 1000 * 60 * 30;

    private static final long FALLBACK_CLEAN_JOB_RESIDUAL_TIME = 180 * 1000;

    private static final int FALLBACK_CORE_CLEAN_THREADS_LOWER_BOUND = 1;

    private static final int FALLBACK_MAX_CLEAN_THREADS_LOWER_BOUND = 1;
    private static final int FALLBACK_MAX_CLEAN_THREADS_UPPER_BOUND = 30;

    private static final int FALLBACK_CLEAN_QUEUE_SIZE = 10000;

    private String urls;
    @ScmRewritableConfMarker
    private long cleanJobPeriod = 1000L * 60 * 30;
    @ScmRewritableConfMarker
    private long cleanJobResidualTime = 180L * 1000L;
    @ScmRewritableConfMarker
    private int coreCleanThreads = 3;
    @ScmRewritableConfMarker
    private int maxCleanThreads = 6;
    @ScmRewritableConfMarker
    private int cleanQueueSize = 10000;
    private int maxBuffer;
    private ZkAcl acl = new ZkAcl();

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public long getCleanJobPeriod() {
        return cleanJobPeriod;
    }

    @PostConstruct
    public void adjustCoreCleanThreads() {
        if (coreCleanThreads > maxCleanThreads) {
            logger.warn(
                    "coreCleanThreads:{} is greater than maxCleanThreads:{}, update coreCleanThreads to:{}",
                    coreCleanThreads, maxCleanThreads, maxCleanThreads);
            coreCleanThreads = maxCleanThreads;
        }
        else if (coreCleanThreads < 0) {
            logger.warn("Invalid coreCleanThreads value: " + coreCleanThreads
                    + ", set to minimum value: " + 1);
            coreCleanThreads = 1;
        }
    }

    public void setCleanJobPeriod(long cleanJobPeriod) {
        if (cleanJobPeriod <= 1000) {
            logger.warn("Invalid cleanJobPeriod value: {}, set to fallback value: {}.",
                    cleanJobPeriod, FALLBACK_CLEAN_JOB_PERIOD);
            cleanJobPeriod = FALLBACK_CLEAN_JOB_PERIOD;
        }

        this.cleanJobPeriod = cleanJobPeriod;
    }

    public long getCleanJobResidualTime() {
        return cleanJobResidualTime;
    }

    public void setCleanJobResidualTime(long cleanJobResidualTime) {
        if (cleanJobResidualTime <= 1000) {
            logger.warn("Invalid cleanJobResidualTime value: {}, set to fallback value: {}.",
                    cleanJobResidualTime, FALLBACK_CLEAN_JOB_RESIDUAL_TIME);
            cleanJobResidualTime = FALLBACK_CLEAN_JOB_RESIDUAL_TIME;
        }

        this.cleanJobResidualTime = cleanJobResidualTime;
    }

    public int getMaxCleanThreads() {
        return maxCleanThreads;
    }

    public void setMaxCleanThreads(int maxCleanThreads) {
        if (maxCleanThreads < 1) {
            logger.warn("Invalid maxCleanThreads value: {}, set to fallback value: {}.",
                    maxCleanThreads, FALLBACK_MAX_CLEAN_THREADS_LOWER_BOUND);
            this.maxCleanThreads = FALLBACK_MAX_CLEAN_THREADS_LOWER_BOUND;
        }
        else if (maxCleanThreads > 30) {
            logger.warn("Invalid maxCleanThreads value: {}, set to fallback value: {}.",
                    maxCleanThreads, FALLBACK_MAX_CLEAN_THREADS_UPPER_BOUND);
            this.maxCleanThreads = FALLBACK_MAX_CLEAN_THREADS_UPPER_BOUND;
        }
        else {
            this.maxCleanThreads = maxCleanThreads;
        }
    }

    public int getCoreCleanThreads() {
        return coreCleanThreads;
    }

    public void setCoreCleanThreads(int coreCleanThreads) {
        if (coreCleanThreads < 0) {
            logger.warn("Invalid coreCleanThreads value: {}, set to fallback value: {}.",
                    coreCleanThreads, FALLBACK_CORE_CLEAN_THREADS_LOWER_BOUND);
            coreCleanThreads = FALLBACK_CORE_CLEAN_THREADS_LOWER_BOUND;
        }
        else if (coreCleanThreads > maxCleanThreads) {
            logger.warn("Invalid coreCleanThreads value: {}, set to fallback value: {}.",
                    coreCleanThreads, maxCleanThreads);
            coreCleanThreads = maxCleanThreads;
        }
        this.coreCleanThreads = coreCleanThreads;
    }

    public int getCleanQueueSize() {
        return cleanQueueSize;
    }

    public void setCleanQueueSize(int cleanQueueSize) {
        if (cleanQueueSize < 1) {
            logger.warn("Invalid cleanQueueSize value: {}, set to fallback value: {}.",
                    cleanQueueSize, FALLBACK_CLEAN_QUEUE_SIZE);
            cleanQueueSize = FALLBACK_CLEAN_QUEUE_SIZE;
        }
        this.cleanQueueSize = cleanQueueSize;
    }

    public int getMaxBuffer() {
        return maxBuffer;
    }

    public void setMaxBuffer(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    public ZkAcl getAcl() {
        return acl;
    }

    public void setAcl(ZkAcl acl) {
        this.acl = acl;
    }
}
