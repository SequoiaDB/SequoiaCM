package com.sequoiacm.mq.client.remote;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.MessageInternal;

@RequestMapping("/internal/v1/msg_queue")
public interface ConsumerFeignClient {

    @PostMapping(value = "/partition?" + CommonDefine.REST_ACTION + "=" + CommonDefine.REST_ACQUIRE)
    public boolean acquiresPartition(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String consumerGroup,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer) throws MqException;

    @PostMapping(value = "/partition?" + CommonDefine.REST_ACTION + "=" + CommonDefine.REST_RELEASE)
    public void releasesPartition(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String consumerGroup,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer) throws MqException;

    @PostMapping(value = "/msg?" + CommonDefine.REST_ACTION + "=" + CommonDefine.REST_ACTION_ACK)
    public void commitMsg(@RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(value = CommonDefine.REST_ACK_PARTITION) int commitPartition,
            @RequestParam(value = CommonDefine.REST_ACK_MSGS) List<Long> commitedMsgs) throws MqException;

    @PostMapping(value = "/msg?" + CommonDefine.REST_ACTION + "=" + CommonDefine.REST_ACTION_PULL)
    public List<MessageInternal> pullMsg(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(value = CommonDefine.REST_MSG_PULL_SIZE, required = false, defaultValue = "10") int pullSize,
            @RequestParam(value = CommonDefine.REST_ACK_PARTITION, required = false, defaultValue = "-1") int commitPartition,
            @RequestParam(value = CommonDefine.REST_ACK_MSGS, required = false) List<Long> commitedMsgs)
            throws MqException;

    @PostMapping(value = "/msg?"+CommonDefine.REST_ACTION + "="
            + CommonDefine.REST_ACTION_FEEDBACK)
    public void feedback(@RequestParam(CommonDefine.REST_TOPIC) String topic,
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_MSG_ID) long msgId,
            @RequestParam(CommonDefine.REST_KEY) String msgKey,
            @RequestParam(CommonDefine.REST_MSG_FEEDBACK) BSONObject feedback) throws MqException;
}
