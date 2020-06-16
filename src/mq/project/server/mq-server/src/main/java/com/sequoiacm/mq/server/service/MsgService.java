package com.sequoiacm.mq.server.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.MessageInternal;

public interface MsgService {

    public List<MessageInternal> pullMsg(String consumerGroup, String consumer, int pullMaxRetSize,
            int ackPartitionNum, List<Long> ackMsg) throws MqException;

    public void putMsg(String topic, String key, BSONObject content) throws MqException;

    public void commitMsg(String consumerGroup, String consumer, int partitionNum,
            List<Long> commitMsg) throws MqException;

}
