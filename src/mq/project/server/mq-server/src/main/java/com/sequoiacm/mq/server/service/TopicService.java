package com.sequoiacm.mq.server.service;

import java.util.List;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.Topic;

public interface TopicService {
    public Topic getTopic(String topic) throws MqException;

    public List<Topic> getTopics() throws MqException;

    public void createTopic(String topic, int partitionCount) throws MqException;

    public void deleteTopic(String topic) throws MqException;

    public void updateTopicPartition(String topicName, int newPartitionCount, long timeout)
            throws MqException;
}
