package com.sequoiacm.mq.client.core;

import com.sequoiacm.mq.client.remote.ProducerFeignClient;
import com.sequoiacm.mq.core.exception.MqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerClient {
    private static final Logger logger = LoggerFactory.getLogger(ProducerClient.class);
    private final String producerAddr;
    private final ProducerLockManager lockMgr;
    private ProducerFeignClient client;
    private FeedbackCallbackMgr feedbackCallbackMgr;

    public ProducerClient(String producerAddr, ProducerFeignClient client,
            FeedbackCallbackMgr feedbackCallbackMgr, ProducerLockManager lockMgr) {
        this.client = client;
        this.feedbackCallbackMgr = feedbackCallbackMgr;
        this.producerAddr = producerAddr;
        this.lockMgr = lockMgr;
    }

    public long putMsg(String topic, String key, SerializableMessage m) throws MqException {
        return client.putMsg(topic, key, m.serialize().toString(), null);
    }

    public long putMsg(String topic, String key, SerializableMessage m,
            long registerCallbackTimeout, List<FeedbackCallback<?>> callbacks)
            throws MqException, InterruptedException {
        if (callbacks == null || callbacks.isEmpty()) {
            return putMsg(topic, key, m);
        }

        // 投递消息和注册回调在一把锁内，找 feedbackCallbackMgr 获取回调也需要加这把锁，
        // 防止在投递消息与注册回调之间的空隙，有线程来找回调对象
        ReentrantLock lock = lockMgr.getLock(key);
        lock.lock();
        try {
            long msgId = client.putMsg(topic, key, m.serialize().toString(), producerAddr);
            feedbackCallbackMgr.registerCallback(topic, key, msgId, callbacks,
                    registerCallbackTimeout);
            return msgId;
        }
        finally {
            lock.unlock();
        }
    }

    public long putMsg(String topic, String key, SerializableMessage m,
            List<FeedbackCallback<?>> callbacks) throws MqException, InterruptedException {
        return putMsg(topic, key, m, Long.MAX_VALUE, callbacks);
    }

    public void triggerCallbackTimeout(String topic, String group, long lessThanOrEqualsMsgId,
            long waitTime) throws InterruptedException {
        feedbackCallbackMgr.triggerTimeout(topic, group, lessThanOrEqualsMsgId, waitTime);
    }

    public long putMsg(String topic, String key, SerializableMessage m,
            long registerCallbackTimeout, FeedbackCallback<?> callback)
            throws MqException, InterruptedException {
        if (callback == null) {
            return putMsg(topic, key, m);
        }
        List<FeedbackCallback<?>> list = new ArrayList<>(1);
        list.add(callback);
        return putMsg(topic, key, m, registerCallbackTimeout, list);
    }

    public long putMsg(String topic, String key, SerializableMessage m,
            FeedbackCallback<?> callback) throws MqException, InterruptedException {
        return putMsg(topic, key, m, Long.MAX_VALUE, callback);
    }
}
