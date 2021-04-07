package com.sequoiacm.cloud.gateway.statistics.config;

import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "scm.statistics.types." + ScmStatisticsType.FILE_UPLOAD
        + ".conditions")
public class ScmStatisticsFileUploadConfig extends ScmStatisticsFileConfig {

}
