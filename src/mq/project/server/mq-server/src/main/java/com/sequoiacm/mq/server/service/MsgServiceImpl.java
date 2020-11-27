package com.sequoiacm.mq.server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.core.module.MessageInternal;
import com.sequoiacm.mq.core.module.Topic;
import com.sequoiacm.mq.server.common.CircularIterator;
import com.sequoiacm.mq.server.dao.ConsumerGroupRepository;
import com.sequoiacm.mq.server.dao.MsgRepository;
import com.sequoiacm.mq.server.dao.PartitionRepository;
import com.sequoiacm.mq.server.dao.TopicRepository;
import com.sequoiacm.mq.server.lock.LockManager;
import com.sequoiacm.mq.server.lock.LockPathFactory;

@Service
public class MsgServiceImpl implements MsgService {
    private static final Logger logger = LoggerFactory.getLogger(MsgServiceImpl.class);
    @Autowired
    private MsgRepository msgRepository;
    @Autowired
    private ConsumerGroupRepository consumerGroupRepository;

    @Autowired
    private PartitionRepository partitionRep;

    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private LockManager lockMgr;
    @Autowired
    private LockPathFactory lockPathFactory;

    private void applyAck(ConsumerPartitionInfo ackPartition, List<Long> ackMsg)
            throws MqException {
        Collections.sort(ackMsg);
        long lastDeliveredId = ackPartition.getLastDeliveredId();
        if (lastDeliveredId >= ackMsg.get(ackMsg.size() - 1)) {
            logger.warn("message has been consume:ackMsg={}, partitionInfo={}", ackMsg,
                    ackPartition);
            return;
        }

        List<Long> pendingMsg = ackPartition.getPendingMsgs();
        if (pendingMsg == null || pendingMsg.size() <= 0) {
            logger.warn(
                    "ack message is inconsistent with the partition pending msg:ackMsgs={}, partitionInfo={}",
                    ackMsg, ackPartition);
            return;
        }

        if (ackMsg.equals(pendingMsg)) {
            Long lastDeleveredId = ackMsg.get(ackMsg.size() - 1);
            partitionRep.changePartitionLastDeleveredId(null, ackPartition.getConsumerGroup(),
                    ackPartition.getPartitionNum(), lastDeleveredId);
            ackPartition.setLastDeliveredId(lastDeleveredId);
            ackPartition.setPendingMsgs(new ArrayList<Long>());
            return;
        }
        logger.warn(
                "ack message is inconsistent with the partition pending msg:ackMsgs={}, partitionInfo={}",
                ackMsg, ackPartition);
    }

    private List<MessageInternal> pullMsg(String topicName, Iterator<ConsumerPartitionInfo> it,
            int maxRetSize) throws MqException {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new MqException(MqError.TOPIC_NOT_EXIST, "topic not exist:" + topicName);
        }
        while (it.hasNext()) {
            ConsumerPartitionInfo partition = it.next();
            List<MessageInternal> msgs = msgRepository.getMsg(topic.getMessageTableName(),
                    partition.getPartitionNum(), partition.getLastDeliveredId(), maxRetSize);
            if (msgs.size() > 0) {
                List<Long> pendingMsg = new ArrayList<>();
                for (MessageInternal m : msgs) {
                    pendingMsg.add(m.getId());
                }
                partitionRep.changePartitionPendingMsg(null, partition.getConsumerGroup(),
                        partition.getPartitionNum(), pendingMsg);
                return msgs;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<MessageInternal> pullMsg(String consumerGroup, String consumer, int pullMaxRetSize,
            int ackPartNum, List<Long> ackMsg) throws MqException {
        ConsumerGroup group = consumerGroupRepository.getGroupByName(consumerGroup);
        if (group == null) {
            throw new MqException(MqError.CONSUMER_GROUP_NOT_EXIST,
                    "consumer group not exist:group=" + consumerGroup);
        }
        ScmLock readLock = lockMgr
                .acquiresReadLock(lockPathFactory.pullMsgLockPath(group.getTopic()));
        try {
            List<ConsumerPartitionInfo> partitions = partitionRep.getPartitions(consumerGroup,
                    consumer);
            if (partitions.size() <= 0) {
                throw new MqException(MqError.NO_PARTITION_FOR_CONSUMER,
                        "no partition for consumer:topic=" + group.getTopic() + ", consumerGroup="
                                + consumerGroup + ", consumer=" + consumer);
            }
            Collections.sort(partitions, PartitionNumComparator.INSTANCE);

            if (ackPartNum >= 0) {
                int ackPartIdx = indexOfPartition(partitions, ackPartNum);
                if (ackPartIdx != -1) {
                    if (ackMsg != null && ackMsg.size() > 0) {
                        applyAck(partitions.get(ackPartIdx), ackMsg);
                        discardPendingMsg(partitions);
                    }

                    int thisReqPartStartIdx = ackPartIdx + 1;
                    if (thisReqPartStartIdx > partitions.size() - 1) {
                        thisReqPartStartIdx = 0;
                    }
                    CircularIterator<ConsumerPartitionInfo> it = new CircularIterator<>(partitions,
                            thisReqPartStartIdx);
                    return pullMsg(group.getTopic(), it, pullMaxRetSize);
                }
                logger.warn("akc partition is not belong to the consumer:ackPartNum=" + ackPartNum
                        + ", consumer=" + consumer + ", consumerPartitions=" + partitions);
            }
            discardPendingMsg(partitions);
            Iterator<ConsumerPartitionInfo> it = partitions.iterator();
            return pullMsg(group.getTopic(), it, pullMaxRetSize);
        }
        finally {
            readLock.unlock();
        }
    }

    private void discardPendingMsg(List<ConsumerPartitionInfo> partitions) throws MqException {
        for (ConsumerPartitionInfo p : partitions) {
            if (p.getPendingMsgs() != null && p.getPendingMsgs().size() > 0) {
                logger.warn("discard pending msg:partition={}", p);
                partitionRep.discardPartitionPendingMsg(null, p.getConsumerGroup(),
                        p.getPartitionNum());
            }
        }
    }

    @Override
    public long putMsg(String topicName, String key, BSONObject content) throws MqException {
        int keyHash = Math.abs(key.hashCode());
        MessageInternal msg = new MessageInternal();
        msg.setCreateTime(System.currentTimeMillis());
        msg.setKey(key);
        msg.setMsgContent(content);
        msg.setTopic(topicName);
        ScmLock readLock = lockMgr.acquiresReadLock(lockPathFactory.putMsgLockPath(topicName));
        try {
            Topic topic = topicRepository.getTopic(topicName);
            if (topic == null) {
                throw new MqException(MqError.TOPIC_NOT_EXIST,
                        "topic not exist:topic=" + topicName);
            }
            msg.setPartition(keyHash % topic.getPartitionCount());
            return msgRepository.putMsg(topic.getMessageTableName(), msg);
        }
        finally {
            readLock.unlock();
        }
    }

    private int indexOfPartition(List<ConsumerPartitionInfo> partitions, int partitionNum) {
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).getPartitionNum() == partitionNum) {
                return i;
            }

            if (partitions.get(i).getPartitionNum() > partitionNum) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public void commitMsg(String consumerGroup, String consumer, int partitionNum,
            List<Long> commitMsg) throws MqException {
        ConsumerGroup group = consumerGroupRepository.getGroupByName(consumerGroup);
        if (group == null) {
            throw new MqException(MqError.CONSUMER_GROUP_NOT_EXIST,
                    "consumer group not exist:group=" + consumerGroup);
        }
        ScmLock readLock = lockMgr
                .acquiresReadLock(lockPathFactory.pullMsgLockPath(group.getTopic()));
        try {
            List<ConsumerPartitionInfo> partitions = partitionRep.getPartitions(consumerGroup,
                    consumer);
            if (partitions.size() <= 0) {
                throw new MqException(MqError.NO_PARTITION_FOR_CONSUMER,
                        "no partition for consumer:topic=" + group.getTopic() + ", consumerGroup="
                                + consumerGroup + ", consumer=" + consumer);
            }
            Collections.sort(partitions, PartitionNumComparator.INSTANCE);

            if (partitionNum >= 0) {
                int partitionIndex = indexOfPartition(partitions, partitionNum);
                if (partitionIndex != -1) {
                    if (commitMsg != null && commitMsg.size() > 0) {
                        applyAck(partitions.get(partitionIndex), commitMsg);
                        discardPendingMsg(partitions);
                    }
                }
            }
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public MessageInternal peekLatestMessage(String topicName) throws MqException {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new MqException(MqError.TOPIC_NOT_EXIST, "topic not exist:topic=" + topicName);
        }
        MessageInternal msg = msgRepository.getMaxIdMsg(topic.getMessageTableName(), -1);
        if (msg == null) {
            throw new MqException(MqError.TOPIC_IS_EMPTY, "topic is empty:" + topicName);
        }
        return msg;
    }

    @Override
    public boolean checkMsgConsumed(String topicName, String group, long msgId,
            boolean ensureLteMsgConsumed) throws MqException {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            throw new MqException(MqError.TOPIC_NOT_EXIST, "topic not exist:topic=" + topicName);
        }
        List<ConsumerPartitionInfo> partitionList = partitionRep.getPartitionByGroup(group);
        if (partitionList.isEmpty()) {
            throw new MqException(MqError.CONSUMER_GROUP_NOT_EXIST,
                    "consumer group not exist:" + group);
        }
        Collections.sort(partitionList, new Comparator<ConsumerPartitionInfo>() {
            @Override
            public int compare(ConsumerPartitionInfo o1, ConsumerPartitionInfo o2) {
                return o1.getLastDeliveredId() - o2.getLastDeliveredId() > 0 ? 1 : -1;
            }
        });
        if (partitionList.get(0).getLastDeliveredId() >= msgId) {
            /**
             * 各分区消费情况如下
             * p1->10
             * p2->15
             * p3->16
             * 此时查询 9 号及之前的消息是否被消费，所有分区都已经超过 9 号，返回 true
             */
            return true;
        }
        if (partitionList.get(partitionList.size() - 1).getLastDeliveredId() < msgId) {
            /**
             * 各分区消费情况如下
             * p1->10
             * p2->15
             * p3->16
             * 此时查询 18 号及之前的消息是否被消费，所有分区都未超过 18 号，返回 false
             */
            return false;
        }

        if (ensureLteMsgConsumed) {
            /**
             * 各分区消费情况如下
             * p1->10
             * p2->20
             * p3->30
             * 此时查询 28 号及之前的消息是否被消费，同时满足如下条件可以确定 28 号及之前的消息已被消费：
             * 1、p1分区不包含 (10, 28] 号消息
             * 2、p2分区不包含 (20, 28] 号消息
             */
            for (ConsumerPartitionInfo p : partitionList) {
                if (p.getLastDeliveredId() >= msgId) {
                    continue;
                }
                long count = msgRepository.getMsgCount(topic.getMessageTableName(),
                        p.getPartitionNum(), p.getLastDeliveredId(), msgId);
                if (count > 0) {
                    return false;
                }
            }
            return true;
        }

        // 单独查询某条消息是否被消费，只要看该消息所在分区的消费进度即可
        MessageInternal msg = msgRepository.getMsg(topic.getMessageTableName(), msgId);
        if (msg == null) {
            logger.info("no such msg, assume msg has been consumed:topic={}, msgId={}", topicName,
                    msgId);
            return true;
        }
        for (ConsumerPartitionInfo p : partitionList) {
            if (p.getPartitionNum() != msg.getPartition()) {
                continue;
            }
            if (p.getLastDeliveredId() >= msgId) {
                return true;
            }
            return false;
        }

        return true;
    }

}

class PartitionNumComparator implements Comparator<ConsumerPartitionInfo> {
    static final PartitionNumComparator INSTANCE = new PartitionNumComparator();

    @Override
    public int compare(ConsumerPartitionInfo o1, ConsumerPartitionInfo o2) {
        return o1.getPartitionNum() - o2.getPartitionNum();
    }

}