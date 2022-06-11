package com.sequoiacm.cloud.gateway.config;

import com.sequoiacm.infrastructure.dispatcher.ScmRestClient;
import com.sequoiacm.infrastructure.dispatcher.ScmRestClientConfig;
import com.sequoiacm.infrastructure.dispatcher.ScmShortCircuitRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScmRestClientConfiguration {

    @Bean
    public ScmRestClient scmDispatcher(UploadForwardConfig config) {
        ScmRestClientConfig restClientConfig = new ScmRestClientConfig();
        restClientConfig.setConnectionRequestTimeout(config.getConnectionRequestTimeout());
        restClientConfig.setConnectTimeout(config.getConnectTimeout());
        restClientConfig
                .setConnectionCleanerRepeatInterval(config.getConnectionCleanerRepeatInterval());
        restClientConfig.setSocketTimeout(config.getSocketTimeout());
        restClientConfig.setConnectionTimeToLive(config.getConnectionTimeToLive());
        restClientConfig.setMaxPerRouteConnections(config.getMaxPerRouteConnections());
        restClientConfig.setMaxTotalConnections(config.getMaxTotalConnections());
        return new ScmShortCircuitRestClient(restClientConfig);
    }
}
