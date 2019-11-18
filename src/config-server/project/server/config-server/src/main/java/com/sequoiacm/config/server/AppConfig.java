package com.sequoiacm.config.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class AppConfig{
    @Autowired
    LockConfig lockConfig;

    public LockConfig getLockConfig() {
        return lockConfig;
    }


}

@Component
@ConfigurationProperties(prefix = "scm.zookeeper")
class LockConfig {
    String urls;
    int clientNum = 10;
    long cleanJobPeriod = 12L * 3600L * 1000L;
    long cleanJobResidualTime = 24L * 3600L * 1000L;

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public int getClientNum() {
        return clientNum;
    }

    public void setClientNum(int clientNum) {
        this.clientNum = clientNum;
    }

    public long getCleanJobPeriod() {
        return cleanJobPeriod;
    }

    public void setCleanJobPeriod(long cleanJobPeriod) {
        this.cleanJobPeriod = cleanJobPeriod;
    }

    public long getCleanJobResidualTime() {
        return cleanJobResidualTime;
    }

    public void setCleanJobResidualTime(long cleanJobResidualTime) {
        this.cleanJobResidualTime = cleanJobResidualTime;
    }

}
