package com.sequoiacm.mq.server.dao;

import java.util.List;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.Topic;

public interface TopicRepository {

    public void createTopic(Transaction transaction, Topic topic) throws MqException;

    public void deleteTopic(Transaction transaction, String topicName) throws MqException;

    public Topic getTopic(String topicName) throws MqException;

    public List<Topic> getTopics() throws MqException;

    public void updateTopic(Transaction transaction, String topicName, int newPartitionCount)
            throws MqException;
}
