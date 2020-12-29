package com.sequoiacm.mq.client.core;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FeedbackCallbackMgr {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackCallbackMgr.class);
    private final int callbackSize;
    private final long callbackTimeout;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Condition callbackRemoveCondition = rwLock.writeLock().newCondition();

    // key is topic+consumerGroup+msgId
    private final Map<FeedbackWrapperKey, FeedbackCallbackWrapper<?>> callbacks = new HashMap<>();

    private final ScmTimer timer = ScmTimerFactory.createScmTimer();

    public FeedbackCallbackMgr(int callbackSize, long callbackTimeout, long clearPeriod) {
        this.callbackSize = callbackSize;
        this.callbackTimeout = callbackTimeout;
        timer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                Lock rLock = rwLock.readLock();
                rLock.lock();
                HashSet<FeedbackCallbackWrapper<?>> callbackCopy;
                try {
                    callbackCopy = new HashSet<>(callbacks.values());
                }
                finally {
                    rLock.unlock();
                }
                removeTimeoutCallback(callbackCopy);
            }
        }, clearPeriod, clearPeriod);
    }

    @PreDestroy
    public void destroy() {
        timer.cancel();
    }

    public FeedbackCallbackWrapper<?> getCallback(String topicName, String grp, long msgId) {
        FeedbackWrapperKey key = new FeedbackWrapperKey(topicName, grp, msgId);
        Lock rLock = rwLock.readLock();
        rLock.lock();
        try {
            return callbacks.get(key);
        }
        finally {
            rLock.unlock();
        }
    }

    void removeCallback(String topicName, String grp, long msgId) {
        FeedbackWrapperKey key = new FeedbackWrapperKey(topicName, grp, msgId);
        Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            callbacks.remove(key);
            callbackRemoveCondition.signalAll();
        }
        finally {
            writeLock.unlock();
        }
    }

    public void registerCallback(String topicName, String msgKey, long msgId,
            List<FeedbackCallback<?>> callbackList, long timeout)
            throws InterruptedException, MqException {
        long start = System.currentTimeMillis();
        Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            while (callbacks.size() > callbackSize - callbackList.size()) {
                removeTimeoutCallback(new HashSet<>(callbacks.values()));
                if (callbacks.size() <= callbackSize) {
                    break;
                }
                if (System.currentTimeMillis() - start > timeout) {
                    throw new MqException(MqError.PRODUCER_CLIENT_REGISTER_CALLBACK_TIMEOUT,
                            "failed to register feedback callback, cause by timeout:topicName="
                                    + topicName + ", msgId=" + msgId + ", msgKey=" + msgKey
                                    + ", timeout=" + timeout + ", maxCallbackSize=" + callbackSize);
                }
                callbackRemoveCondition.await(1000, TimeUnit.MILLISECONDS);
            }
            for (FeedbackCallback<?> c : callbackList) {
                FeedbackCallbackWrapper<?> callbackWrapper = new FeedbackCallbackWrapper<>(c, this,
                        topicName, msgKey, msgId);
                FeedbackWrapperKey key = new FeedbackWrapperKey(topicName, c.getListenWhichGrp(),
                        msgId);
                callbacks.put(key, callbackWrapper);
            }
        }
        finally {
            writeLock.unlock();
        }
    }

    public void triggerTimeout(String topic, String group, long lessThanOrEqualsMsgId) {
        Lock rLock = rwLock.readLock();
        rLock.lock();
        HashSet<FeedbackCallbackWrapper<?>> callbackCopy;
        try {
            callbackCopy = new HashSet<>(callbacks.values());
        }
        finally {
            rLock.unlock();
        }
        for (FeedbackCallbackWrapper<?> c : callbackCopy) {
            if (c.getTopic().equals(topic) && c.getGroupName().equals(group)
                    && c.getMsgId() <= lessThanOrEqualsMsgId) {
                c.innerOnTimeout();
            }
        }
    }

    private void removeTimeoutCallback(Set<FeedbackCallbackWrapper<?>> ks) {
        long now = System.currentTimeMillis();
        for (FeedbackCallbackWrapper<?> c : ks) {
            if (now - c.getStartTime() > callbackTimeout) {
                c.innerOnTimeout();
            }
        }
    }
}

class FeedbackWrapperKey {
    private final String topic;
    private final String group;
    private final long msgId;

    FeedbackWrapperKey(String topic, String group, long msgId) {
        this.topic = topic;
        this.group = group;
        this.msgId = msgId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FeedbackWrapperKey that = (FeedbackWrapperKey) o;
        return msgId == that.msgId && topic.equals(that.topic) && group.equals(that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, group, msgId);
    }

    @Override
    public String toString() {
        return "FeedbackWrapperKey{" + "topic='" + topic + '\'' + ", group='" + group + '\''
                + ", msgId=" + msgId + '}';
    }
}
