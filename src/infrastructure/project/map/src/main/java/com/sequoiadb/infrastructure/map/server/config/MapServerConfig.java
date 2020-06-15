package com.sequoiadb.infrastructure.map.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.map.cache")
public class MapServerConfig {
    private long maxResidualTime = 2 * 3600 * 1000;
    private long clearJobPeriod = 1 * 3600 * 1000;

    public long getMaxResidualTime() {
        return maxResidualTime;
    }

    public void setMaxResidualTime(long maxResidualTime) {
        this.maxResidualTime = maxResidualTime;
    }

    public long getClearJobPeriod() {
        return clearJobPeriod;
    }

    public void setClearJobPeriod(long clearJobPeriod) {
        this.clearJobPeriod = clearJobPeriod;
    }

}
