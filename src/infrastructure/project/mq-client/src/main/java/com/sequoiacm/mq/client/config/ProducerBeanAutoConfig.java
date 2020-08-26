package com.sequoiacm.mq.client.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.mq.client.core.ProducerClient;
import com.sequoiacm.mq.client.remote.ProducerFeignClient;
import com.sequoiacm.mq.core.exception.FeignExceptionConverter;

@Configuration
@ConditionalOnBean(ProducerConfig.Marker.class)
public class ProducerBeanAutoConfig {

    @Bean
    public ProducerClient producerClient(ScmFeignClient feignClient) {
        ProducerFeignClient feign = feignClient.builder()
                .exceptionConverter(new FeignExceptionConverter())
                .serviceTarget(ProducerFeignClient.class, AdminBeanAutoConfig.SERVICE_NAME);
        return new ProducerClient(feign);
    }
}
