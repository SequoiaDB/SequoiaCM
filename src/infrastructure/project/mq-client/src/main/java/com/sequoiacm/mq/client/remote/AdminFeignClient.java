package com.sequoiacm.mq.client.remote;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupDetail;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.core.module.MessageInternal;
import com.sequoiacm.mq.core.module.TopicDetail;

@RequestMapping("/internal/v1")
public interface AdminFeignClient {
    @PostMapping(value = "/msg_queue/topics")
    public void createTopic(@RequestParam(CommonDefine.REST_NAME) String topicName,
            @RequestParam(CommonDefine.REST_PARTITION_COUNT) int partitionCount) throws MqException;

    @PutMapping(value = "/msg_queue/topics")
    public void updateTopic(@RequestParam(CommonDefine.REST_NAME) String topicName,
            @RequestParam(CommonDefine.REST_PARTITION_COUNT) int newPartitionCount,
            @RequestParam(value = CommonDefine.REST_TIMEOUT, required = false, defaultValue = Long.MAX_VALUE
                    + "") long timeout,
            @RequestParam(CommonDefine.REST_ACTION) String action) throws MqException;

    @DeleteMapping(value = "/msg_queue/topics/{topicname}")
    public void deleteTopic(@PathVariable("topicname") String topicName) throws MqException;

    @GetMapping(value = "/msg_queue/topics/{topicname}")
    public TopicDetail getTopic(@PathVariable("topicname") String topicName) throws MqException;

    @GetMapping(value = "/msg_queue/topics")
    public List<TopicDetail> getTopics() throws MqException;

    @PostMapping(value = "/msg_queue/consumer_groups")
    public void createGroup(@RequestParam(CommonDefine.REST_TOPIC) String topic,
            @RequestParam(CommonDefine.REST_NAME) String groupName,
            @RequestParam(CommonDefine.REST_OFFSET) ConsumerGroupOffsetEnum offset)
            throws MqException;

    @GetMapping(value = "/msg_queue/consumer_groups/{consumer_group}")
    public ConsumerGroupDetail getGroup(@PathVariable("consumer_group") String groupName)
            throws MqException;

    @DeleteMapping(value = "/msg_queue/consumer_groups/{consumer_group}")
    public void deleteGroup(@PathVariable("consumer_group") String groupName) throws MqException;

    @GetMapping(value = "/msg_queue/consumer_groups")
    public List<ConsumerGroupDetail> getGroups(@RequestParam(CommonDefine.REST_TOPIC) String topic)
            throws MqException;

    @GetMapping(value = "/msg_queue/msg")
    public MessageInternal peekLatestMsg(@RequestParam(CommonDefine.REST_TOPIC) String topic)
            throws MqException;

    @GetMapping(value = "/msg_queue/msg?" + CommonDefine.REST_ACTION + "="
            + CommonDefine.REST_ACTION_CHECK_CONSUMED)
    public boolean checkMsgConsumed(
            @RequestParam(CommonDefine.REST_TOPIC) String topic,
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_MSG_ID) long msgId,
            @RequestParam(value = CommonDefine.REST_ENSURE_LESS_THAN_OR_EQ_MSG_BE_CONSUMED, defaultValue = "false") boolean ensureLteMsgConsumed)
            throws MqException;
}
