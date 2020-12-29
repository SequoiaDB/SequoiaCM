package com.sequoiacm.mq.server.dao;

import java.util.List;
import java.util.Map;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;

public interface PartitionRepository {
    public void deletePartitionByTopic(Transaction t, String topic) throws MqException;

    public void deletePartitionByGroup(Transaction t, String group) throws MqException;

    public void deletePartition(Transaction t, String topic, int num) throws MqException;

    public void createPartition(Transaction t, String topic, String group, int num,
            long initLastDeleveredId, String consumer) throws MqException;

    public List<ConsumerPartitionInfo> getPartitionByGroup(String groupName) throws MqException;

    public List<ConsumerPartitionInfo> getPartitionByTopicAndNum(String topicName, int partitionNum) throws MqException;

    public Map<Integer, List<ConsumerPartitionInfo>> getPartitionByTopic(String topicName)
            throws MqException;

    public List<ConsumerPartitionInfo> getPartitions(String groupName, String consumer)
            throws MqException;

    public void changePartitionConsumer(Transaction t, String groupName, int num,
            String newConsumer) throws MqException;

    public void changePartitionPendingMsg(Transaction t, String groupName, int num,
            List<Long> newPendingMsg) throws MqException;

    public void changePartitionLastDeleveredId(Transaction t, String groupName, int num,
            Long newLastDeleveredId) throws MqException;

    public void updatePartitionRequestTime(Transaction t, String groupName, int num)
            throws MqException;

    public void discardPartitionPendingMsg(Transaction t, String groupName, int num)
            throws MqException;

}
