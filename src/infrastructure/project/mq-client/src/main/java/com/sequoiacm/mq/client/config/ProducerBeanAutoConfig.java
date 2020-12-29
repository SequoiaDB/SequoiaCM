package com.sequoiacm.mq.client.config;

import com.sequoiacm.mq.client.controller.ProducerController;
import com.sequoiacm.mq.client.core.FeedbackCallbackMgr;
import com.sequoiacm.mq.client.core.ProducerLockManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.mq.client.core.ProducerClient;
import com.sequoiacm.mq.client.remote.ProducerFeignClient;
import com.sequoiacm.mq.core.exception.FeignExceptionConverter;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@ConditionalOnBean(ProducerConfig.Marker.class)
public class ProducerBeanAutoConfig {

    @Value("${server.port}")
    private int producerPort;

    @Value("${scm.mq.client.producer.feedbackCallbackTimeout:1800000}")
    private long feedbackCallbackTimeout = 1800000;

    @Value("${scm.mq.client.producer.feedbackCallbackCleanPeriod:300000}")
    private long feedbackCallbackCleanPeriod = 300000;

    @Value("${scm.mq.client.producer.maxFeedbackCallbacks:50000}")
    private int maxFeedbackCallbacks = 50000;

    @Value("${scm.mq.client.producer.putMsgLocks:200}")
    private int producerLockSize = 200;

    @Bean
    public FeedbackCallbackMgr feedbackListenerMgr() {
        return new FeedbackCallbackMgr(maxFeedbackCallbacks, feedbackCallbackTimeout,
                feedbackCallbackCleanPeriod);
    }

    @Bean
    public ProducerController producerController(FeedbackCallbackMgr callbackMgr, ProducerLockManager lockMgr) {
        return new ProducerController(callbackMgr, lockMgr);
    }

    @Bean
    public ProducerClient producerClient(ScmFeignClient feignClient, FeedbackCallbackMgr mgr,
            ProducerLockManager lockMgr) throws UnknownHostException {
        ProducerFeignClient feign = feignClient.builder()
                .exceptionConverter(new FeignExceptionConverter())
                .serviceTarget(ProducerFeignClient.class, AdminBeanAutoConfig.SERVICE_NAME);
        return new ProducerClient(InetAddress.getLocalHost().getHostName() + ":" + producerPort,
                feign, mgr, lockMgr);
    }

    @Bean
    public ProducerLockManager producerLockManager() {
        return new ProducerLockManager(producerLockSize);
    }
}
