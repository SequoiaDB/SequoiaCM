package com.sequoiacm.mq.server.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.core.module.Topic;
import com.sequoiacm.mq.core.module.TopicDetail;
import com.sequoiacm.mq.server.service.ConsumerGroupService;
import com.sequoiacm.mq.server.service.TopicService;

@RequestMapping("/internal/v1")
@RestController
public class TopicController {
    @Autowired
    private TopicService service;
    @Autowired
    private ConsumerGroupService consumerGroupService;

    @PostMapping(value = "/msg_queue/topics")
    public void createTopic(@RequestParam(CommonDefine.REST_NAME) String topicName,
            @RequestParam(CommonDefine.REST_PARTITION_COUNT) int partitionCount)
            throws MqException {
        if (partitionCount < 1) {
            throw new MqException(MqError.INVALID_ARG,
                    "partition count must be greater than or equals 1");
        }
        service.createTopic(topicName, partitionCount);
    }

    @PutMapping(value = "/msg_queue/topics", params = CommonDefine.REST_ACTION + "="
            + CommonDefine.REST_ACTION_UPDATE_PARTITION_COUNT)
    public void updateTopic(@RequestParam(CommonDefine.REST_NAME) String topicName,
            @RequestParam(CommonDefine.REST_PARTITION_COUNT) int newPartitionCount,
            @RequestParam(value = CommonDefine.REST_TIMEOUT, required = false, defaultValue = Long.MAX_VALUE
                    + "") long timeout)
            throws MqException {
        if (newPartitionCount < 1) {
            throw new MqException(MqError.INVALID_ARG,
                    "partition count must be greater than or equals 1");
        }
        if (timeout < 0) {
            timeout = Long.MAX_VALUE;
        }
        service.updateTopicPartition(topicName, newPartitionCount, timeout);
    }

    @DeleteMapping(value = "/msg_queue/topics/{topicname:.+}")
    public void deleteTopic(@PathVariable("topicname") String topicName) throws MqException {
        service.deleteTopic(topicName);
    }

    @GetMapping(value = "/msg_queue/topics/{topicname:.+}")
    public TopicDetail getTopic(@PathVariable("topicname") String topicName) throws MqException {
        Topic topic = service.getTopic(topicName);
        if (topic == null) {
            throw new MqException(MqError.TOPIC_NOT_EXIST, "topic not exist:topic=" + topicName);
        }
        List<ConsumerGroup> groups = consumerGroupService.getGroupsByTopic(topicName);
        ArrayList<String> groupNames = new ArrayList<>(groups.size());
        for (ConsumerGroup g : groups) {
            groupNames.add(g.getName());
        }
        return new TopicDetail(topic, groupNames);
    }

    @GetMapping(value = "/msg_queue/topics")
    public List<TopicDetail> getTopics() throws MqException {
        List<Topic> topics = service.getTopics();
        ArrayList<TopicDetail> ret = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            List<ConsumerGroup> groups = consumerGroupService.getGroupsByTopic(topic.getName());
            ArrayList<String> groupNames = new ArrayList<>(groups.size());
            for (ConsumerGroup g : groups) {
                groupNames.add(g.getName());
            }
            ret.add(new TopicDetail(topic, groupNames));
        }
        return ret;
    }

}
