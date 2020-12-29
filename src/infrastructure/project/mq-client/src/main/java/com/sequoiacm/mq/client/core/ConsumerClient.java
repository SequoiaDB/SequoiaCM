package com.sequoiacm.mq.client.core;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.Message;
import com.sequoiacm.mq.core.module.MessageInternal;

import feign.RetryableException;

public class ConsumerClient<M, F> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerClient.class);
    private final FeedbackSerializer<F> feedbackSerializer;
    private final MessageDeserializer<M> ds;
    private final String group;
    private final String consumer;
    private boolean havePartition;
    private long lastAcquirePartitionTime;
    private int lastPullMsgPartition = -1;
    private final List<Long> lastPullMsgIds = new ArrayList<>();
    private final ConsumerClientMgr mgr;
    private boolean newMsgArrive = false;
    private boolean hasPullMsgLastTime = true;

    public ConsumerClient(ConsumerClientMgr mgr, String group, String consumer,
            MessageDeserializer<M> s, FeedbackSerializer<F> feedbackSerializer) throws MqException {
        this.ds = s;
        this.mgr = mgr;
        this.group = group;
        this.consumer = consumer;
        this.havePartition = mgr.getFeignClient().acquiresPartition(group, consumer);
        this.lastAcquirePartitionTime = System.currentTimeMillis();
        this.feedbackSerializer = feedbackSerializer;
    }

    public synchronized void notifyNewMsgArrive() {
        newMsgArrive = true;
        this.notify();
    }

    public String getConsumerGroup() {
        return group;
    }

    public List<Message<M>> pullMsg(int size, long timeout)
            throws MqException, InterruptedException {
        long interval = Math.min(timeout, mgr.getConsumerConfProperties().getPullMsgInterval());
        long start = System.currentTimeMillis();
        synchronized (this) {
            boolean needPullMsg = hasPullMsgLastTime || newMsgArrive;
            while (true) {
                if (needPullMsg) {
                    List<Message<M>> ret = pullMsg(size);
                    newMsgArrive = false;
                    if (ret != null) {
                        hasPullMsgLastTime = true;
                        return ret;
                    }
                    hasPullMsgLastTime = false;
                }
                if (System.currentTimeMillis() - start > timeout) {
                    return null;
                }
                this.wait(interval);
                needPullMsg = true;
            }
        }
    }

    public List<Message<M>> pullMsg(int size) throws MqException {
        if (!havePartition) {
            havePartition = acquirePartition();
            if (!havePartition) {
                return null;
            }
        }

        List<MessageInternal> msgs;
        try {
            msgs = mgr.getFeignClient().pullMsg(group, consumer, size, lastPullMsgPartition,
                    lastPullMsgIds);
        }
        catch (RetryableException e) {
            logger.warn("failed to connect message queue server", e);
            return null;
        }
        catch (MqException e) {
            if (e.getError() == MqError.NO_PARTITION_FOR_CONSUMER) {
                logger.warn("consumer lost all partition when pulling msg:consumerGroup={}", group,
                        e);
                havePartition = false;
                lastAcquirePartitionTime = System.currentTimeMillis();
                lastPullMsgIds.clear();
                return null;
            }
            throw e;
        }

        lastPullMsgIds.clear();

        if (msgs != null && msgs.size() > 0) {
            List<Message<M>> ret = new ArrayList<>();
            for (MessageInternal m : msgs) {
                Message<M> retMessage = new Message<>();
                retMessage.setCreateTime(m.getCreateTime());
                retMessage.setId(m.getId());
                retMessage.setKey(m.getKey());
                retMessage.setMsgContent(ds.deserialize(m.getMsgContent()));
                retMessage.setPartition(m.getPartition());
                retMessage.setTopic(m.getTopic());
                retMessage.setMsgProducer(m.getMsgProducer());
                lastPullMsgIds.add(m.getId());
                ret.add(retMessage);
            }
            lastPullMsgPartition = ret.get(0).getPartition();
            return ret;
        }
        return null;
    }

    private boolean acquirePartition() throws MqException {
        long now = System.currentTimeMillis();
        if (now - lastAcquirePartitionTime < mgr.getConsumerConfProperties()
                .getAcquirePartitionInterval()) {
            return havePartition;
        }
        lastAcquirePartitionTime = now;
        try {
            havePartition = mgr.getFeignClient().acquiresPartition(group, consumer);
        }
        catch (feign.RetryableException e) {
            logger.warn("failed to connect message queue server", e);
        }
        return havePartition;
    }

    public void feedbackSilence(Message<M> feedbackForMsg, F feedback) {
        int retryTimes = 3;
        while (retryTimes-- > 0) {
            try {
                mgr.getFeignClient().feedback(feedbackForMsg.getTopic(), group,
                        feedbackForMsg.getId(), feedbackForMsg.getKey(),
                        feedbackSerializer.serialize(feedback));
                return;
            }
            catch (Exception e) {
                logger.warn("failed to send feedback:msg={}, feedback={}", feedbackForMsg, feedback,
                        e);
            }
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                logger.warn("retry to send feedback failed cause by interrupt:msg={}, feedback={}",
                        feedbackForMsg, feedback, e);
                return;
            }
        }
    }

    @Override
    public void close() {
        try {
            if (!havePartition) {
                return;
            }
            if (lastPullMsgIds.size() > 0) {
                mgr.getFeignClient().commitMsg(group, consumer, lastPullMsgPartition,
                        lastPullMsgIds);
            }
            mgr.getFeignClient().releasesPartition(group, consumer);
        }
        catch (Exception e) {
            logger.warn("failed to commit msg and release partition:group=" + group + ", consumer="
                    + consumer, e);
        }
        finally {
            mgr.releaseClient(this);
        }
    }

}
