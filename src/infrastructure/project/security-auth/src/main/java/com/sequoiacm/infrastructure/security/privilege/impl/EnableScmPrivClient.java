package com.sequoiacm.infrastructure.security.privilege.impl;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients("com.sequoiacm.infrastructure.security.privilege.impl")
@EnableDiscoveryClient
@ComponentScan("com.sequoiacm.infrastructure.security.privilege.impl")
public @interface EnableScmPrivClient {
}
