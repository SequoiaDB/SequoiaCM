package com.sequoiacm.mq.client.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.mq.client.remote.AdminFeignClient;
import com.sequoiacm.mq.core.exception.FeignExceptionConverter;

@Configuration
@ConditionalOnBean(AdminConfig.Marker.class)
public class AdminBeanAutoConfig {
    public static final String SERVICE_NAME = "mq-server";

    @Bean
    public AdminClient adminClient(ScmFeignClient feignClient) {
        AdminFeignClient feign = feignClient.builder()
                .exceptionConverter(new FeignExceptionConverter())
                .serviceTarget(AdminFeignClient.class, SERVICE_NAME);
        return new AdminClient(feign);
    }
}
