package com.sequoiacm.mq.server.service;

import java.util.List;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.core.module.ConsumerGroup;

public interface ConsumerGroupService {
    public List<ConsumerGroup> getGroupsByTopic(String topic) throws MqException;

    public List<ConsumerGroup> getAllGroup() throws MqException;

    public ConsumerGroup getGroupByName(String groupName) throws MqException;

    public void createGroup(String topic, String groupName, ConsumerGroupOffsetEnum consumePosition) throws MqException;

    public void deleteGroup(String groupName) throws MqException;

}
