package com.sequoiacm.cloud.gateway.statistics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "scm.statistics.types." + ScmStatisticsType.FILE_UPLOAD
        + ".conditions")
public class ScmStatisticsFileUploadConfig extends ScmStatisticsFileConfig {

}
