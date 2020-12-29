package com.sequoiacm.mq.client.controller;

import com.sequoiacm.mq.client.core.FeedbackCallback;
import com.sequoiacm.mq.client.core.FeedbackCallbackMgr;
import com.sequoiacm.mq.client.core.FeedbackCallbackWrapper;
import com.sequoiacm.mq.client.core.ProducerLockManager;
import com.sequoiacm.mq.core.CommonDefine;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.locks.ReentrantLock;

@RequestMapping("/internal/v1")
@ResponseBody
public class ProducerController {
    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    private final FeedbackCallbackMgr feedbackCallbackMgr;
    private final ProducerLockManager lockMgr;

    public ProducerController(FeedbackCallbackMgr mgr, ProducerLockManager lockMgr) {
        this.feedbackCallbackMgr = mgr;
        this.lockMgr = lockMgr;
    }

    @PostMapping("/msg_queue/client/feedback")
    public void feedback(@RequestParam(CommonDefine.REST_TOPIC) String topic,
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_MSG_ID) long msgId,
            @RequestParam(CommonDefine.REST_KEY) String msgKey,
            @RequestParam(CommonDefine.REST_MSG_FEEDBACK) BSONObject feedback) {
        FeedbackCallbackWrapper<?> callback;

        // 投递消息和注册回调这两个动作在也会加这把锁，
        // 防止在投递消息与注册回调之间的空隙，有线程来找回调对象
        ReentrantLock lock = lockMgr.getLock(msgKey);
        lock.lock();
        try {
            callback = feedbackCallbackMgr.getCallback(topic, group, msgId);
        }
        finally {
            lock.unlock();
        }
        if (callback == null) {
            logger.warn(
                    "failed to process feedback, no such callback:topicName={}, group={}, msgId={}, msgKey={}, feedback={}",
                    topic, group, msgId, msgKey, feedback);
            return;
        }
        try {
            callback.innerOnFeedback(feedback);
        }
        catch (Exception e) {
            logger.warn(
                    "failed to process feedback:topicName={}, group={}, msgId={}, msgKey={}, feedback={}",
                    topic, group, msgKey, msgId, feedback, e);
        }
    }
}
