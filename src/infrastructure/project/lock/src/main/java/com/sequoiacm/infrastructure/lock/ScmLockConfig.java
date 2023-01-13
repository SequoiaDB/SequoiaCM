package com.sequoiacm.infrastructure.lock;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;

@ConfigurationProperties(prefix = "scm.zookeeper")
public class ScmLockConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScmLockConfig.class);
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
    private boolean disableJob = false;

    public void setDisableJob(boolean disableJob) {
        this.disableJob = disableJob;
    }

    public boolean isDisableJob() {
        return disableJob;
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

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public long getCleanJobPeriod() {
        return cleanJobPeriod;
    }

    public void setCleanJobPeriod(long cleanJobPeriod) {
        if (cleanJobPeriod <= 1000) {
            logger.warn("Invalid cleanJobPeriod value: " + cleanJobPeriod
                    + ", set to default value: " + this.cleanJobPeriod);
            return;
        }

        this.cleanJobPeriod = cleanJobPeriod;
    }

    public long getCleanJobResidualTime() {
        return cleanJobResidualTime;
    }

    public void setCleanJobResidualTime(long cleanJobResidualTime) {
        if (cleanJobResidualTime <= 1000) {
            logger.warn("Invalid cleanJobResidualTime value: " + cleanJobResidualTime
                    + ", set to default value: " + this.cleanJobResidualTime);
            return;
        }

        this.cleanJobResidualTime = cleanJobResidualTime;
    }

    public int getMaxCleanThreads() {
        return maxCleanThreads;
    }

    public void setMaxCleanThreads(int maxCleanThreads) {
        if (maxCleanThreads < 1) {
            logger.warn("Invalid maxCleanThreads value: " + maxCleanThreads
                    + ", set to minimum value: " + 1);
            this.maxCleanThreads = 1;
        }
        else if (maxCleanThreads > 30) {
            logger.warn("Invalid maxCleanThreads value: " + maxCleanThreads
                    + ", set to maximum value: " + 30);
            this.maxCleanThreads = 30;
        }
        else {
            this.maxCleanThreads = maxCleanThreads;
        }
    }

    public int getCleanQueueSize() {
        return cleanQueueSize;
    }

    public void setCleanQueueSize(int cleanQueueSize) {
        if (cleanQueueSize < 1) {
            logger.warn("Invalid cleanQueueSize value: " + cleanQueueSize
                    + ", set to default value: " + this.cleanQueueSize);
            return;
        }
        this.cleanQueueSize = cleanQueueSize;
    }

    public int getCoreCleanThreads() {
        return coreCleanThreads;
    }

    public void setCoreCleanThreads(int coreCleanThreads) {
        this.coreCleanThreads = coreCleanThreads;
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
