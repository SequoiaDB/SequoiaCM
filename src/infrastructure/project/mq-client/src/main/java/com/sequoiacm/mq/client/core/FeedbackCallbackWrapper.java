package com.sequoiacm.mq.client.core;

import com.sequoiacm.mq.core.exception.MqException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedbackCallbackWrapper<F> {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackCallbackWrapper.class);
    private final String topic;
    private final long msgId;
    private final long startTime;
    private boolean hasTriggerCallback = false;
    private final String key;
    private final FeedbackCallbackMgr feedbackCallbackMgr;
    private volatile FeedbackCallback<F> callback;

    public FeedbackCallbackWrapper(FeedbackCallback<F> callback,
            FeedbackCallbackMgr feedbackCallbackMgr, String topic, String key, long msgId) {
        this.feedbackCallbackMgr = feedbackCallbackMgr;
        this.topic = topic;
        this.msgId = msgId;
        this.key = key;
        this.startTime = System.currentTimeMillis();
        this.callback = callback;
        this.callback.attachWrapper(this);
    }

    public long getMsgId() {
        return msgId;
    }

    public String getGroupName() {
        return callback.getListenWhichGrp();
    }

    public String getTopic() {
        return topic;
    }

    public long getStartTime() {
        return startTime;
    }

    public synchronized void innerOnFeedback(BSONObject feedbackContent) throws MqException {
        if (hasTriggerCallback) {
            logger.debug("ignore feedback:topic={}, group={}, key={}, msgId={}, feedback={}", topic,
                    callback.getListenWhichGrp(), key, msgId, feedbackContent);
            return;
        }
        hasTriggerCallback = true;
        logger.debug("receive feedback:topic={}, group={}, key={}, msgId={}, feedback={}", topic,
                callback.getListenWhichGrp(), key, msgId, feedbackContent);
        try {
            F f = callback.convert(feedbackContent);
            callback.onFeedback(topic, key, msgId, f);
        }
        finally {
            removeFromCallbackMgr();
            this.notify();
        }
    }

    public synchronized void innerOnTimeout() {
        if (hasTriggerCallback) {
            logger.debug("ignore onTimeout:topic={}, group={}, key={}, msgId={}", topic,
                    callback.getListenWhichGrp(), key, msgId);
            return;
        }
        logger.debug("feedback onTimeout:topic={}, group={}, key={}, msgId={}", topic,
                callback.getListenWhichGrp(), key, msgId);
        hasTriggerCallback = true;
        try {
            callback.onTimeout(topic, key, msgId);
        }
        finally {
            removeFromCallbackMgr();
            this.notify();
        }
    }

    void removeFromCallbackMgr() {
        feedbackCallbackMgr.removeCallback(topic, callback.getListenWhichGrp(), msgId);
    }

    public synchronized boolean waitBeTriggered(long timeout) throws InterruptedException {
        if (hasTriggerCallback) {
            return true;
        }
        this.wait(timeout);
        return hasTriggerCallback;
    }
}
