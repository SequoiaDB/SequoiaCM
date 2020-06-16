package com.sequoiacm.mq.server.dao;

import java.util.List;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroup;

public interface ConsumerGroupRepository {
    public void createGroup(Transaction t, String topicName, String groupName) throws MqException;

    public void deleteGroupByTopic(Transaction t, String topicName) throws MqException;

    public void deleteGroupByName(Transaction t, String groupName) throws MqException;

    public List<ConsumerGroup> getGroupByTopic(String topic) throws MqException;

    public List<ConsumerGroup> getAllGroup() throws MqException;

    public ConsumerGroup getGroupByName(String groupName) throws MqException;

}
