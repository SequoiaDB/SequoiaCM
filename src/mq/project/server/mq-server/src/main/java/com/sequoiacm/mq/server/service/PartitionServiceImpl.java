package com.sequoiacm.mq.server.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.server.common.PartitionDistibutor;
import com.sequoiacm.mq.server.dao.ConsumerGroupRepository;
import com.sequoiacm.mq.server.dao.PartitionRepository;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.TransactionFactory;
import com.sequoiacm.mq.server.lock.LockManager;
import com.sequoiacm.mq.server.lock.LockPathFactory;

@Service
public class PartitionServiceImpl implements PartitionService {

    @Autowired
    private TransactionFactory transactionFactory;
    @Autowired
    private ConsumerGroupRepository consumerGrpRepository;

    @Autowired
    private PartitionRepository partitionRep;
    @Autowired
    private LockManager lockMgr;

    @Autowired
    private LockPathFactory lockPathFactory;

    @Override
    public boolean acquiresPartition(String groupName, String consumer) throws MqException {
        ConsumerGroup group = consumerGrpRepository.getGroupByName(groupName);
        if (group == null) {
            throw new MqException(MqError.CONSUMER_GROUP_NOT_EXIST,
                    "group not exist:group=" + groupName);
        }
        List<ConsumerPartitionInfo> consumerPartitionInfo = partitionRep
                .getPartitionByGroup(groupName);
        if (consumerPartitionInfo.size() <= 0) {
            throw new MqException(MqError.CONSUMER_GROUP_NOT_EXIST,
                    "group not exist:group=" + groupName);
        }
        PartitionDistibutor partitionDistributor = new PartitionDistibutor(consumerPartitionInfo);
        if (partitionDistributor.hasPartition(consumer)) {
            return true;
        }
        if (!partitionDistributor.hasPartitionForNewConsumer()) {
            return false;
        }

        ScmLock writeLock = lockMgr
                .acquiresWriteLock(lockPathFactory.pullMsgLockPath(group.getTopic()));
        Transaction transaction = null;
        try {
            consumerPartitionInfo = partitionRep.getPartitionByGroup(groupName);
            partitionDistributor = new PartitionDistibutor(consumerPartitionInfo);
            if (partitionDistributor.hasPartition(consumer)) {
                return true;
            }

            if (!partitionDistributor.hasPartitionForNewConsumer()) {
                return false;
            }
            Map<Integer, String> modifier = partitionDistributor.acquiresPartition(consumer);
            transaction = transactionFactory.createTransaction();
            transaction.begin();
            for (Entry<Integer, String> m : modifier.entrySet()) {
                partitionRep.changePartitionConsumer(transaction, groupName, m.getKey(),
                        m.getValue());
            }
            transaction.commit();
            return true;
        }
        catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void releasePartition(String groupName, String consumer) throws MqException {
        ConsumerGroup group = consumerGrpRepository.getGroupByName(groupName);
        if (group == null) {
            throw new MqException(MqError.CONSUMER_GROUP_NOT_EXIST,
                    "group not exist:group=" + groupName);
        }
        ScmLock writeLock = lockMgr
                .acquiresWriteLock(lockPathFactory.pullMsgLockPath(group.getTopic()));
        Transaction transaction = null;
        try {
            List<ConsumerPartitionInfo> consumerPartitionInfo = partitionRep
                    .getPartitionByGroup(groupName);
            PartitionDistibutor partitionDistributor = new PartitionDistibutor(
                    consumerPartitionInfo);
            Map<Integer, String> modifier = partitionDistributor.rleasePartition(consumer);
            transaction = transactionFactory.createTransaction();
            transaction.begin();
            for (Entry<Integer, String> entry : modifier.entrySet()) {
                partitionRep.changePartitionConsumer(transaction, groupName, entry.getKey(),
                        entry.getValue());
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
            writeLock.unlock();
        }
    }

    @Override
    public List<ConsumerPartitionInfo> getPartitionInfo(String groupName) throws MqException {
        return partitionRep.getPartitionByGroup(groupName);
    }

    @Override
    public List<ConsumerPartitionInfo> getPartitionInfo(String groupName, String consumer)
            throws MqException {
        return partitionRep.getPartitions(groupName, consumer);
    }
}
