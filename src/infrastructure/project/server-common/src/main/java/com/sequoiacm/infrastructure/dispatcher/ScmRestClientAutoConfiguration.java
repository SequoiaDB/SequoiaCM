package com.sequoiacm.infrastructure.dispatcher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(ScmRestClient.class)
@EnableConfigurationProperties(ScmRestClientConfig.class)
public class ScmRestClientAutoConfiguration {

    @Bean
    public ScmRestClient scmDispatcher(ScmRestClientConfig config) {
        return new ScmShortCircuitRestClient(config);
    }

}
