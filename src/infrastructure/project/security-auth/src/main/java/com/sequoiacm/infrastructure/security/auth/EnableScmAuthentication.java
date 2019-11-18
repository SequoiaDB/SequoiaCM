package com.sequoiacm.infrastructure.security.auth;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients(basePackages = "com.sequoiacm.infrastructure.security.auth")
//@EnableFeignClients(basePackages = "com.sequoiacm.cloud.security.authentication")
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.sequoiacm.infrastructure.security.auth")
public @interface EnableScmAuthentication {
}
