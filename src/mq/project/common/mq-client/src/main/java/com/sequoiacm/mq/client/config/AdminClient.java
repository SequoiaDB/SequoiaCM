package com.sequoiacm.mq.client.config;

import java.util.List;

import com.sequoiacm.mq.client.remote.AdminFeignClient;
import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupDetail;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.core.module.TopicDetail;

public class AdminClient {
    private AdminFeignClient client;

    public AdminClient(AdminFeignClient client) {
        this.client = client;
    }

    public void createTopicIfNotExist(String topic, int partitionNum) throws MqException {
        try {
            client.createTopic(topic, partitionNum);
        }
        catch (MqException e) {
            if (e.getError() != MqError.TOPIC_EXIST) {
                throw e;
            }
        }
    }

    public void createTopic(String topic, int partitionNum) throws MqException {
        client.createTopic(topic, partitionNum);
    }

    public void createGroup(String group, String topic, ConsumerGroupOffsetEnum p)
            throws MqException {
        client.createGroup(topic, group, p);
    }

    public void createGroup(String group, String topic) throws MqException {
        client.createGroup(topic, group, ConsumerGroupOffsetEnum.OLDEST);
    }

    public ConsumerGroupDetail getGroup(String group) throws MqException {
        return client.getGroup(group);
    }

    public TopicDetail getTopic(String name) throws MqException {
        return client.getTopic(name);
    }

    public void createGroupIfNotExist(String group, String topic, ConsumerGroupOffsetEnum p)
            throws MqException {
        try {
            client.createGroup(topic, group, p);
        }
        catch (MqException e) {
            if (e.getError() == MqError.CONSUMER_GROUP_EXIST) {
                ConsumerGroupDetail g = client.getGroup(group);
                if (g.getTopic().equals(topic)) {
                    return;
                }
            }
            throw e;
        }
    }

    public void deleteGroup(String group) throws MqException {
        client.deleteGroup(group);
    }

    public void deleteTopic(String topic) throws MqException {
        client.deleteTopic(topic);
    }

    public void updateTopicPartitionCount(String topic, int partitionNum, long timeout)
            throws MqException {
        client.updateTopic(topic, partitionNum, timeout,
                CommonDefine.REST_ACTION_UPDATE_PARTITION_COUNT);
    }

    public List<TopicDetail> listTopic() throws MqException {
        return client.getTopics();
    }

    public List<ConsumerGroupDetail> listGroup() throws MqException {
        return client.getGroups();
    }

}
