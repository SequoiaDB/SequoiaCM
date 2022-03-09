package com.sequoiacm.cloud.servicecenter.config;

import com.sequoiacm.infrastructure.metasource.SdbDataSourceAutoConfig;
import com.sequoiacm.infrastructure.metasource.template.SequoiadbTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SdbTemplateConfig {

    // ensure initialization after SdbDataSourceAutoConfig
    @Bean
    public SequoiadbTemplate sequoiadbTemplate(SdbDataSourceAutoConfig sdbDataSourceAutoConfig) {
        return new SequoiadbTemplate();
    }
}
