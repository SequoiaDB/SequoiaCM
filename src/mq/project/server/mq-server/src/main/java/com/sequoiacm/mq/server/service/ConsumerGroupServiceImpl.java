package com.sequoiacm.mq.server.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.core.module.MessageInternal;
import com.sequoiacm.mq.core.module.Topic;
import com.sequoiacm.mq.server.dao.ConsumerGroupRepository;
import com.sequoiacm.mq.server.dao.MsgRepository;
import com.sequoiacm.mq.server.dao.PartitionRepository;
import com.sequoiacm.mq.server.dao.TopicRepository;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.TransactionFactory;
import com.sequoiacm.mq.server.lock.LockManager;
import com.sequoiacm.mq.server.lock.LockPathFactory;

@Service
public class ConsumerGroupServiceImpl implements ConsumerGroupService {
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private ConsumerGroupRepository consumerGroupRepository;

    @Autowired
    private TransactionFactory transactionFactory;

    @Autowired
    private PartitionRepository partitionRep;

    @Autowired
    private LockManager lockMgr;

    @Autowired
    private MsgRepository msgRepository;

    @Autowired
    private LockPathFactory lockPathFactory;

    @Override
    public List<ConsumerGroup> getGroupsByTopic(String topic) throws MqException {
        return consumerGroupRepository.getGroupByTopic(topic);
    }

    @Override
    public ConsumerGroup getGroupByName(String name) throws MqException {
        return consumerGroupRepository.getGroupByName(name);
    }

    @Override
    public void createGroup(String topic, String name, ConsumerGroupOffsetEnum consumePosition)
            throws MqException {
        if (name == null || name.isEmpty() || !name.matches("\\w+")) {
            throw new MqException(MqError.INVALID_ARG, "group name is irregular:name=" + name);
        }
        ScmLock readLock = lockMgr.acquiresReadLock(lockPathFactory.pullMsgLockPath(topic));
        Transaction transaction = null;
        try {
            Topic t = topicRepository.getTopic(topic);
            if (t == null) {
                throw new MqException(MqError.TOPIC_NOT_EXIST, "topic not exist:topic=" + topic);
            }
            transaction = transactionFactory.createTransaction();
            transaction.begin();
            consumerGroupRepository.createGroup(transaction, topic, name);
            for (int i = 0; i < t.getPartitionCount(); i++) {
                if (consumePosition == ConsumerGroupOffsetEnum.OLDEST) {
                    partitionRep.createPartition(transaction, topic, name, i, -1, null);
                    continue;
                }
                MessageInternal maxIdMsg = msgRepository.getMaxIdMsg(t.getMessageTableName(), i);
                if (maxIdMsg == null) {
                    partitionRep.createPartition(transaction, topic, name, i, -1, null);
                }
                else {
                    partitionRep.createPartition(transaction, topic, name, i, maxIdMsg.getId(),
                            null);
                }
            }
            transaction.commit();
        }
        catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
        finally {
            readLock.unlock();
        }

    }

    @Override
    public void deleteGroup(String name) throws MqException {
        ConsumerGroup group = consumerGroupRepository.getGroupByName(name);
        if (group == null) {
            return;
        }
        // 防止主题分区数增加时，导致删除组会残留分区消费记录
        ScmLock readLock = lockMgr
                .acquiresReadLock(lockPathFactory.pullMsgLockPath(group.getTopic()));

        Transaction transaction = null;
        try {
            transaction = transactionFactory.createTransaction();
            transaction.begin();
            consumerGroupRepository.deleteGroupByName(transaction, name);
            partitionRep.deletePartitionByGroup(transaction, name);
            transaction.commit();
        }
        catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public List<ConsumerGroup> getAllGroup() throws MqException {
        return consumerGroupRepository.getAllGroup();
    }

}
