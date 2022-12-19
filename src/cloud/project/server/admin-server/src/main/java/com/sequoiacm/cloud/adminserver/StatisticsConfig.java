package com.sequoiacm.cloud.adminserver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "scm.statistics")
public class StatisticsConfig {

    @ScmRefreshableConfigMarker
    private ScmTimeAccuracy timeGranularity = ScmTimeAccuracy.DAY;

    public ScmTimeAccuracy getTimeGranularity() {
        return timeGranularity;
    }

    public void setTimeGranularity(ScmTimeAccuracy timeGranularity) {
        this.timeGranularity = timeGranularity;
    }
}
