package com.sequoiacm.cloud.adminserver;

import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "scm.statistics")
public class StatisticsConfig {
    private ScmTimeAccuracy timeGranularity = ScmTimeAccuracy.DAY;

    public ScmTimeAccuracy getTimeGranularity() {
        return timeGranularity;
    }

    public void setTimeGranularity(ScmTimeAccuracy timeGranularity) {
        this.timeGranularity = timeGranularity;
    }
}
