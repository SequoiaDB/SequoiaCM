package com.sequoiacm.mq.server.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.sequoiacm.mq.server.common.PartitionDistibutor;
import com.sequoiacm.mq.server.dao.ConsumerGroupRepository;
import com.sequoiacm.mq.server.dao.MsgRepository;
import com.sequoiacm.mq.server.dao.PartitionRepository;
import com.sequoiacm.mq.server.dao.TableCreateResult;
import com.sequoiacm.mq.server.dao.TopicRepository;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.TransactionFactory;
import com.sequoiacm.mq.server.lock.LockManager;
import com.sequoiacm.mq.server.lock.LockPathFactory;

@Service
public class TopicServiceImpl implements TopicService {
    private static final Logger logger = LoggerFactory.getLogger(TopicServiceImpl.class);
    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TransactionFactory transactionFactory;
    @Autowired
    private ConsumerGroupRepository consumerGrpRepository;
    @Autowired
    private PartitionRepository partitionRepository;
    @Autowired
    private MsgRepository msgRepository;

    @Autowired
    private LockManager lockMgr;

    @Autowired
    private LockPathFactory lockPathFactory;

    @Override
    public void createTopic(String topicName, int partitionCount) throws MqException {
        if (topicName == null || topicName.isEmpty() || !topicName.matches("\\w+")) {
            throw new MqException(MqError.INVALID_ARG, "topic name is irregular:name=" + topicName);
        }
        Topic topic = topicRepository.getTopic(topicName);
        if (topic != null) {
            throw new MqException(MqError.TOPIC_EXIST, "topic alredy exist:topic" + topicName);
        }
        TableCreateResult tableCreateRes = msgRepository.createMsgTable(topicName);
        topic = new Topic(topicName, partitionCount, tableCreateRes.getTableName(),0L);
        Transaction transaction = null;
        try {
            transaction = transactionFactory.createTransaction();
            transaction.begin();
            topicRepository.createTopic(transaction, topic);
            consumerGrpRepository.deleteGroupByTopic(transaction, topicName);
            transaction.commit();
        }
        catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (!tableCreateRes.isAlreadyExist()) {
                msgRepository.dropMsgTableSilence(tableCreateRes.getTableName());
            }
            throw e;
        }
    }

    @Override
    public void deleteTopic(String topicName) throws MqException {
        Topic topic = topicRepository.getTopic(topicName);
        if (topic == null) {
            return;
        }
        Transaction transaction = null;
        try {
            transaction = transactionFactory.createTransaction();
            transaction.begin();
            topicRepository.deleteTopic(transaction, topicName);
            consumerGrpRepository.deleteGroupByTopic(transaction, topicName);
            partitionRepository.deletePartitionByTopic(transaction, topicName);
            transaction.commit();
        }
        catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
        msgRepository.dropMsgTableSilence(topic.getMessageTableName());
    }

    @Override
    public void updateTopicPartition(String topicName, int newPartitionCount, long timeout)
            throws MqException {
        ScmLock pullMsgWriteLock = null;
        ScmLock putMsgWriteLock = lockMgr
                .acquiresWriteLock(lockPathFactory.putMsgLockPath(topicName));
        try {
            Topic topic = topicRepository.getTopic(topicName);
            if (topic == null) {
                throw new MqException(MqError.TOPIC_NOT_EXIST, "topic not exist:" + topicName);
            }
            if (topic.getPartitionCount() == newPartitionCount) {
                return;
            }
            waitAllMsgConsumed(topic, timeout);

            pullMsgWriteLock = lockMgr
                    .acquiresWriteLock(lockPathFactory.pullMsgLockPath(topicName));

            int changePartitionCount = topic.getPartitionCount() - newPartitionCount;
            if (changePartitionCount > 0) {
                reducePartition(topic, changePartitionCount);
            }
            else {
                addPartition(topic, -changePartitionCount);
            }
        }
        finally {
            if (pullMsgWriteLock != null) {
                pullMsgWriteLock.unlock();
            }
            putMsgWriteLock.unlock();
        }
    }

    private void addPartition(Topic topic, int newPartitionCount) throws MqException {
        List<Integer> newPartitionNums = new ArrayList<>();
        for (int i = topic.getPartitionCount(); i < topic.getPartitionCount()
                + newPartitionCount; i++) {
            newPartitionNums.add(i);
        }
        List<ConsumerGroup> groups = consumerGrpRepository.getGroupByTopic(topic.getName());
        Transaction transaction = transactionFactory.createTransaction();
        try {
            for (ConsumerGroup group : groups) {
                List<ConsumerPartitionInfo> partitons = partitionRepository
                        .getPartitionByGroup(group.getName());
                PartitionDistibutor pd = new PartitionDistibutor(partitons);

                transaction.begin();
                for (Integer newPartitionNum : newPartitionNums) {
                    partitionRepository.createPartition(transaction, topic.getName(),
                            group.getName(), newPartitionNum, topic.getLatestMsgId(), null);
                }

                Map<Integer, String> modifier = pd.addPartition(newPartitionNums);
                for (Entry<Integer, String> entry : modifier.entrySet()) {
                    partitionRepository.changePartitionConsumer(transaction, group.getName(),
                            entry.getKey(), entry.getValue());
                }
            }
            topicRepository.updateTopic(transaction, topic.getName(),
                    topic.getPartitionCount() + newPartitionCount);
            transaction.commit();
        }
        catch (Exception e) {
            transaction.rollback();
            throw e;
        }

    }

    private void reducePartition(Topic topic, int reducePartitionCount) throws MqException {
        Transaction transaction = transactionFactory.createTransaction();
        try {
            transaction.begin();
            // 0 1 2 3
            List<Integer> reducePartitionNums = new ArrayList<>();
            for (int i = topic.getPartitionCount() - 1; i >= topic.getPartitionCount()
                    - reducePartitionCount; i--) {
                reducePartitionNums.add(i);
            }

            List<ConsumerGroup> groups = consumerGrpRepository.getGroupByTopic(topic.getName());
            for (ConsumerGroup group : groups) {
                List<ConsumerPartitionInfo> partitons = partitionRepository
                        .getPartitionByGroup(group.getName());
                PartitionDistibutor pd = new PartitionDistibutor(partitons);
                Map<Integer, String> modifier = pd.removePartition(reducePartitionNums);
                for (Entry<Integer, String> entry : modifier.entrySet()) {
                    partitionRepository.changePartitionConsumer(transaction, group.getName(),
                            entry.getKey(), entry.getValue());
                }
            }

            for (int reducePartitionNum : reducePartitionNums) {
                partitionRepository.deletePartition(transaction, topic.getName(),
                        reducePartitionNum);
            }

            topicRepository.updateTopic(transaction, topic.getName(),
                    topic.getPartitionCount() - reducePartitionCount);
            transaction.commit();
        }
        catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    private void waitAllMsgConsumed(Topic topic, long timeout) throws MqException {
        Map<Integer, Long> partition2LatestMsgId = new HashMap<>();
        for (int p = 0; p < topic.getPartitionCount(); p++) {
            MessageInternal latestMsg = msgRepository.getMaxIdMsg(topic.getMessageTableName(), p);
            if (latestMsg != null) {
                partition2LatestMsgId.put(p, latestMsg.getId());
            }
            else {
                partition2LatestMsgId.put(p, -1L);
            }
        }
        long loopCount = timeout / 2000;
        while (true) {
            if (isAllMsgHasBeenConsumed(partition2LatestMsgId, topic.getName())) {
                return;
            }
            loopCount--;
            if (loopCount <= 0) {
                throw new MqException(MqError.SYSTEM_ERROR,
                        "failed to wait all msg be consumed cause by timeout:topic="
                                + topic.getName() + ", timeout=" + timeout);
            }
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                throw new MqException(MqError.SYSTEM_ERROR,
                        "failed to wait all msg be consumed cause by interrupt:topic="
                                + topic.getName(),
                        e);
            }
        }
    }

    private boolean isAllMsgHasBeenConsumed(Map<Integer, Long> partition2LatestMsgId,
            String topicName) throws MqException {
        Map<Integer, List<ConsumerPartitionInfo>> partitionNum2Partitions = partitionRepository
                .getPartitionByTopic(topicName);
        for (Entry<Integer, List<ConsumerPartitionInfo>> entry : partitionNum2Partitions
                .entrySet()) {
            Long latestMsgId = partition2LatestMsgId.get(entry.getKey());
            for (ConsumerPartitionInfo consumerPartition : entry.getValue()) {
                if (consumerPartition.getLastDeliveredId() < latestMsgId) {
                    logger.info(
                            "message has not been processed:topic={}, consumerGroup={}, partitionNum={}, latestMsg={}, lastDeleveredMsg={}",
                            topicName, consumerPartition.getConsumerGroup(),
                            consumerPartition.getPartitionNum(), latestMsgId,
                            consumerPartition.getLastDeliveredId());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Topic getTopic(String topicName) throws MqException {
        return topicRepository.getTopic(topicName);
    }

    @Override
    public List<Topic> getTopics() throws MqException {
        return topicRepository.getTopics();
    }

}
