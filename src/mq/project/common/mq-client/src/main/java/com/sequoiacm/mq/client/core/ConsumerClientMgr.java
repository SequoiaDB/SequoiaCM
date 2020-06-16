package com.sequoiacm.mq.client.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.sequoiacm.mq.client.controller.ConsumerController;
import com.sequoiacm.mq.client.remote.ConsumerFeignClient;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;

public class ConsumerClientMgr {
    private ConsumerFeignClient feign;
    private ConsumerController consumerHealthController;
    private String consumer;

    public ConsumerClientMgr(ConsumerFeignClient feign, int clientPort,
            ConsumerController consumerController) throws UnknownHostException {
        this.feign = feign;
        this.consumerHealthController = consumerController;
        this.consumer = InetAddress.getLocalHost().getHostName() + ":" + clientPort;
    }

    public synchronized <M> ConsumerClient<M> createClient(String consumerGroup,
            MessageDeseserializer<M> s) throws MqException {
        if (consumerHealthController.isUp(consumerGroup)) {
            throw new MqException(MqError.SYSTEM_ERROR,
                    "can not create tow client for the same consumer group:" + consumerGroup);
        }
        ConsumerClient<M> c = new ConsumerClient<M>(this, consumerGroup, consumer, s);
        consumerHealthController.consumerUp(consumerGroup);
        return c;
    }

    ConsumerFeignClient getFeignClient() {
        return feign;
    }

    void releaseClient(ConsumerClient<?> c) {
        consumerHealthController.consumerDown(c.getConsumerGroup());
    }
}
