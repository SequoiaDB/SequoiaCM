package com.sequoiacm.infrastructure.statistics.client;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;

@RefreshScope
@ConfigurationProperties(prefix = "scm.statistics")
public class ScmStatisticsReporterConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScmStatisticsReporterConfig.class);

    @ScmRewritableConfMarker
    @ScmRefreshableConfigMarker
    private int rawDataCacheSize = 5000;

    @ScmRewritableConfMarker
    @ScmRefreshableConfigMarker
    private int rawDataReportPeriod = 10000;

    public int getRawDataCacheSize() {
        return rawDataCacheSize;
    }

    public void setRawDataCacheSize(int rawDataCacheSize) {
        if (rawDataCacheSize <= 0) {
            logger.warn("reset scm.statistics.rawDataCacheSize from {} to {}", rawDataCacheSize,
                    10);
            rawDataCacheSize = 10;
        }
        this.rawDataCacheSize = rawDataCacheSize;
    }

    public int getRawDataReportPeriod() {
        return rawDataReportPeriod;
    }

    public void setRawDataReportPeriod(int rawDataReportPeriod) {
        if (rawDataReportPeriod <= 0) {
            logger.warn("reset scm.statistics.rawDataReportPeriod from {}ms to {}ms",
                    rawDataReportPeriod, 1000);
            rawDataReportPeriod = 1000;
        }
        this.rawDataReportPeriod = rawDataReportPeriod;
    }
}
