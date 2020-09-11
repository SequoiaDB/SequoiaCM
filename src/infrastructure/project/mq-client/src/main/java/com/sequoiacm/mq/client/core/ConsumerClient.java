package com.sequoiacm.mq.client.core;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.Message;
import com.sequoiacm.mq.core.module.MessageInternal;

import feign.RetryableException;

public class ConsumerClient<M> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerClient.class);
    private long acquirePartitionPeriod = 10000;
    private long pullMsgRetryPeriod = 1000;
    private MessageDeseserializer<M> ds;
    private String group;
    private String consumer;
    private boolean havePartition;
    private long lastAcquirePartitionTime;
    private int lastPullMsgPartition = -1;
    private List<Long> lastPullMsgIds = new ArrayList<>();
    private ConsumerClientMgr mgr;

    public ConsumerClient(ConsumerClientMgr mgr, String group, String consumer,
            MessageDeseserializer<M> s) throws MqException {
        this.ds = s;
        this.mgr = mgr;
        this.group = group;
        this.consumer = consumer;
        this.havePartition = mgr.getFeignClient().acquiresPartition(group, consumer,
                CommonDefine.REST_ACQUIRE);
        this.lastAcquirePartitionTime = System.currentTimeMillis();
    }

    public String getConsumerGroup() {
        return group;
    }

    public void sleep(long time) throws MqException {
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException e) {
            throw new MqException(MqError.INTERRUPT, "failed to wait cause by interrupt", e);
        }
    }

    public List<Message<M>> pullMsg(int size, long timeoutInsecond) throws MqException {
        long timeoutInMs = timeoutInsecond * 1000;
        long startTime = System.currentTimeMillis();
        while (true) {
            try {
                List<Message<M>> ret = pullMsg(size);
                if (ret != null) {
                    return ret;
                }
            }
            catch (MqException e) {
                if (e.getError() == MqError.NO_PARTITION_FOR_CONSUMER) {
                    havePartition = false;
                    lastAcquirePartitionTime = System.currentTimeMillis();
                }
                throw e;
            }
            sleep(pullMsgRetryPeriod);
            if (System.currentTimeMillis() - startTime > timeoutInMs) {
                return null;
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

        List<MessageInternal> msgs = null;
        try {
            msgs = mgr.getFeignClient().pullMsg(group, consumer, CommonDefine.REST_ACTION_PULL,
                    size, lastPullMsgPartition, lastPullMsgIds);
        }
        catch (RetryableException e) {
            logger.warn("failed to connect message queue server", e);
            return null;
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
        if (now - lastAcquirePartitionTime < acquirePartitionPeriod) {
            return havePartition;
        }
        lastAcquirePartitionTime = now;
        try {
            havePartition = mgr.getFeignClient().acquiresPartition(group, consumer,
                    CommonDefine.REST_ACQUIRE);
        }
        catch (feign.RetryableException e) {
            logger.warn("failed to connect message queue server", e);
        }
        return havePartition;
    }

    @Override
    public void close() {
        try {
            if (!havePartition) {
                return;
            }
            if (lastPullMsgIds != null && lastPullMsgIds.size() > 0) {
                mgr.getFeignClient().commitMsg(group, consumer, lastPullMsgPartition,
                        lastPullMsgIds, CommonDefine.REST_ACTION_ACK);
            }
            mgr.getFeignClient().releasesPartition(group, consumer, CommonDefine.REST_RELEASE);
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
