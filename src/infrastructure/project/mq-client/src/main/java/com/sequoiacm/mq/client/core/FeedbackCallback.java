package com.sequoiacm.mq.client.core;

import com.sequoiacm.mq.core.exception.MqException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 本回调的作用：消费者处理消息后，通过消息队列发送给生产者的通知，由本回调进行处理
public abstract class FeedbackCallback<F> {
    private final String listenWhichGrp;

    public FeedbackCallback(String listenWhichGrp){
        this.listenWhichGrp = listenWhichGrp;
    }
    private volatile FeedbackCallbackWrapper<F> wrapper;

    public abstract void onFeedback(String topic, String key, long msgId, F feedbackContent);

    public abstract void onTimeout(String topic, String key, long msgId);

    protected abstract F convert(BSONObject feedback) throws MqException;

    final void attachWrapper(FeedbackCallbackWrapper<F> wrapper) {
        this.wrapper = wrapper;
    }

    public final void unregisterCallback() {
        if (wrapper != null) {
            this.wrapper.removeFromCallbackMgr();
        }
    }

    public String getListenWhichGrp() {
        return listenWhichGrp;
    }
}
