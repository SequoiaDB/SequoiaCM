package com.sequoiadb.infrastructure.map.client;

import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.sequoiadb.infrastructure.map.client")
@EnableFeignClients("com.sequoiadb.infrastructure.map.client.service")
public class MapClientAutoConfig {
}
