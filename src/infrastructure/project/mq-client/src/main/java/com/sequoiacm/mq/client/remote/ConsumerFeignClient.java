package com.sequoiacm.mq.client.remote;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.MessageInternal;

@RequestMapping("/internal/v1/msg_queue")
public interface ConsumerFeignClient {

    @PostMapping(value = "/partition")
    public boolean acquiresPartition(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String consumerGroup,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(CommonDefine.REST_ACTION) String action) throws MqException;

    @PostMapping(value = "/partition")
    public void releasesPartition(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String consumerGroup,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(CommonDefine.REST_ACTION) String action) throws MqException;

    @PostMapping(value = "/msg")
    public void commitMsg(@RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(value = CommonDefine.REST_ACK_PARTITION) int commitPartition,
            @RequestParam(value = CommonDefine.REST_ACK_MSGS) List<Long> commitedMsgs,
            @RequestParam(CommonDefine.REST_ACTION) String action) throws MqException;

    @PostMapping(value = "/msg")
    public List<MessageInternal> pullMsg(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(CommonDefine.REST_ACTION) String action,
            @RequestParam(value = CommonDefine.REST_MSG_PULL_SIZE, required = false, defaultValue = "10") int pullSize,
            @RequestParam(value = CommonDefine.REST_ACK_PARTITION, required = false, defaultValue = "-1") int commitPartition,
            @RequestParam(value = CommonDefine.REST_ACK_MSGS, required = false) List<Long> commitedMsgs)
            throws MqException;
}
