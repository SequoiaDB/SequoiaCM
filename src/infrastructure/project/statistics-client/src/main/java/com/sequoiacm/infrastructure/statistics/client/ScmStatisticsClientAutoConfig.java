package com.sequoiacm.infrastructure.statistics.client;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import org.springframework.context.annotation.Bean;

public class ScmStatisticsClientAutoConfig {

    @Bean
    public ScmStatisticsClient client(ScmFeignClient feignClient) {
        return new ScmStatisticsClient(feignClient);
    }

    @Bean
    public ScmStatisticsRawDataReporter reporter(ScmStatisticsReporterConfig config,
            ScmStatisticsClient c) {
        return new ScmStatisticsRawDataReporter(config, c);
    }

    @Bean
    public ScmStatisticsReporterConfig config() {
        return new ScmStatisticsReporterConfig();
    }
}
