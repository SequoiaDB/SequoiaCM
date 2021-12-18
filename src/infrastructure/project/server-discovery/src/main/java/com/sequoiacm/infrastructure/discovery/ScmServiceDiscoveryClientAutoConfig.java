package com.sequoiacm.infrastructure.discovery;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@EnableDiscoveryClient
public class ScmServiceDiscoveryClientAutoConfig {
    @Bean
    public ScmServiceDiscoveryClient scmServiceDiscoveryClient(
            DiscoveryClient springDiscoveryClient) {
        return new ScmServiceDiscoveryClient(springDiscoveryClient);
    }
}
