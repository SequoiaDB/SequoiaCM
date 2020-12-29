package com.sequoiacm.mq.client.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.mq.client.config.ConsumerConfProperties;
import com.sequoiacm.mq.client.remote.ConsumerFeignClient;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;

public class ConsumerClientMgr {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerClientMgr.class);
    private final ConsumerConfProperties consumerConfProperties;
    private Map<String, ConsumerClient> consumerClients = new HashMap<>();
    private ReentrantReadWriteLock consumerClientsLock = new ReentrantReadWriteLock();
    private ConsumerFeignClient feign;
    private String consumer;

    public ConsumerClientMgr(ConsumerFeignClient feign, int clientPort,
            ConsumerConfProperties consumerConfProperties) throws UnknownHostException {
        this.feign = feign;
        this.consumer = InetAddress.getLocalHost().getHostName() + ":" + clientPort;
        this.consumerConfProperties = consumerConfProperties;
    }

    public <M, F> ConsumerClient<M, F> createClient(String consumerGroup,
            MessageDeserializer<M> s, FeedbackSerializer<F> feedbackSerializer)
            throws MqException {
        Lock wLock = consumerClientsLock.writeLock();
        wLock.lock();
        try {
            if (consumerClients.containsKey(consumerGroup)) {
                throw new MqException(MqError.SYSTEM_ERROR,
                        "can not create tow client for the same consumer group:" + consumerGroup);
            }
            ConsumerClient<M, F> c = new ConsumerClient<M, F>(this, consumerGroup, consumer, s,
                    feedbackSerializer);
            consumerClients.put(consumerGroup, c);
            return c;
        }
        finally {
            wLock.unlock();
        }
    }

    ConsumerFeignClient getFeignClient() {
        return feign;
    }

    void releaseClient(ConsumerClient<?, ?> c) {
        Lock wLock = consumerClientsLock.writeLock();
        wLock.lock();
        try {
            consumerClients.remove(c.getConsumerGroup());
        }
        finally {
            wLock.unlock();
        }
    }

    ConsumerConfProperties getConsumerConfProperties() {
        return consumerConfProperties;
    }

    public ConsumerClient<?, ?> getConsumerClient(String group) {
        Lock rLock = consumerClientsLock.readLock();
        rLock.lock();
        try {
            return consumerClients.get(group);
        }
        finally {
            rLock.unlock();
        }
    }

}
