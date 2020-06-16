package com.sequoiacm.mq.client.config;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.mq.client.controller.ConsumerController;
import com.sequoiacm.mq.client.core.ConsumerClientMgr;
import com.sequoiacm.mq.client.remote.ConsumerFeignClient;
import com.sequoiacm.mq.core.exception.FeignExceptionConverter;

@Configuration
@ConditionalOnBean(ConsumerConfig.Marker.class)
public class ConsumerBeanAutoConfig {

    @Value("${server.port}")
    private int consumerClientPort;

    @Bean
    public ConsumerClientMgr consumerClient(ScmFeignClient feignClient,
            ConsumerController consumerController) throws UnknownHostException {
        ConsumerFeignClient feign = feignClient.builder()
                .exceptionConverter(new FeignExceptionConverter())
                .serviceTarget(ConsumerFeignClient.class, AdminBeanAutoConfig.SERVICE_NAME);
        return new ConsumerClientMgr(feign, consumerClientPort, consumerController);
    }

    @Bean
    public ConsumerController consumerController() throws UnknownHostException {
        return new ConsumerController();
    }
}
