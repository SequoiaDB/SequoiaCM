package com.sequoiacm.contentserver.config;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.CommonDefine;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "scm.zookeeper")
public class ZkConfig {
    private static final Logger logger = LoggerFactory.getLogger(ZkConfig.class);

    private String urls = CommonDefine.DefaultValue.ZK_URL;
    private int lockTimeout = CommonDefine.DefaultValue.ZK_LOCK_TIMEOUT;
    private int clientTimeout = CommonDefine.DefaultValue.ZK_CLIENT_TIMEOUT;
    @ScmRewritableConfMarker
    private long cleanJobPeriod = CommonDefine.DefaultValue.ZK_CLEANJOB_PERIOD;
    @ScmRewritableConfMarker
    private long cleanJobResidualTime = CommonDefine.DefaultValue.ZK_CLEANJOB_RESIDUAL;
    @ScmRewritableConfMarker
    private int maxCleanThreads = CommonDefine.DefaultValue.ZK_MAX_CLEAN_THREAD;
    @ScmRewritableConfMarker
    private int coreCleanThreads = CommonDefine.DefaultValue.ZK_CORE_CLEAN_THREAD;
    @ScmRewritableConfMarker
    private int cleanQueueSize = CommonDefine.DefaultValue.ZK_CLEAN_QUEUE_SIZE;
    private int maxBuffer = CommonDefine.DefaultValue.ZK_MAX_BUFFER;
    private ZkAcl acl = new ZkAcl();

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

    public int getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(int clientTimeout) {
        if (clientTimeout <= 0) {
            logger.warn("Invalid client timeout value: " + clientTimeout
                    + ", set to default value: " + this.clientTimeout);
            return;
        }
        this.clientTimeout = clientTimeout;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public int getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(int lockTimeout) {
        if (lockTimeout <= 0) {
            logger.warn("Invalid lock timeout value: " + lockTimeout + ", set to default value: "
                    + this.lockTimeout);
            return;
        }
        this.lockTimeout = lockTimeout;
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
