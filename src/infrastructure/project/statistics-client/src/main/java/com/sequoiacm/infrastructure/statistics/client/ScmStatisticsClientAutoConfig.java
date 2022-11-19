package com.sequoiacm.infrastructure.statistics.client;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.statistics.client.flush.StatisticsRawDataFlush;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

@ComponentScan(basePackages = "com.sequoiacm.infrastructure.statistics.client.flush")
public class ScmStatisticsClientAutoConfig {

    @Bean
    public ScmStatisticsClient client(ScmFeignClient feignClient) {
        return new ScmStatisticsClient(feignClient);
    }

    @Bean
    public ScmStatisticsRawDataReporter scmStatisticsRawDataReporter(ScmStatisticsReporterConfig config,
            ScmStatisticsClient c, List<StatisticsRawDataFlush> rawDataFlushList) {
        return new ScmStatisticsRawDataReporter(config, c, rawDataFlushList);
    }

    @Bean
    public ScmStatisticsReporterConfig config() {
        return new ScmStatisticsReporterConfig();
    }
}
