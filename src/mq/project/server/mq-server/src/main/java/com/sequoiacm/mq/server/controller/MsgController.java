package com.sequoiacm.mq.server.controller;

import java.util.List;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.MessageInternal;
import com.sequoiacm.mq.server.service.MsgService;

@RequestMapping("/internal/v1/msg_queue")
@RestController
public class MsgController {
    @Autowired
    private MsgService service;

    @PostMapping(value = "/msg", params = CommonDefine.REST_ACTION + "="
            + CommonDefine.REST_ACTION_PUT)
    public long putMsg(@RequestParam(CommonDefine.REST_TOPIC) String topic,
            @RequestParam(CommonDefine.REST_KEY) String key,
            @RequestParam(CommonDefine.REST_MSG_CONTENT) BSONObject content) throws MqException {
        return service.putMsg(topic, key, content);
    }

    @PostMapping(value = "/msg", params = CommonDefine.REST_ACTION + "="
            + CommonDefine.REST_ACTION_ACK)
    public void commitMsg(@RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(value = CommonDefine.REST_ACK_PARTITION) int commitPartition,
            @RequestParam(value = CommonDefine.REST_ACK_MSGS) List<Long> commitedMsgs)
            throws MqException {
        service.commitMsg(group, consumer, commitPartition, commitedMsgs);
    }

    @PostMapping(value = "/msg")
    public List<MessageInternal> pullMsg(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer,
            @RequestParam(value = CommonDefine.REST_MSG_PULL_SIZE, required = false, defaultValue = "10") int pullSize,
            @RequestParam(value = CommonDefine.REST_ACK_PARTITION, required = false, defaultValue = "-1") int ackPartition,
            @RequestParam(value = CommonDefine.REST_ACK_MSGS, required = false) List<Long> ackMsgs)
            throws MqException {
        return service.pullMsg(group, consumer, pullSize, ackPartition, ackMsgs);
    }

    @GetMapping(value = "/msg")
    public MessageInternal peekMsg(@RequestParam(CommonDefine.REST_TOPIC) String topic)
            throws MqException {
        return service.peekLatestMessage(topic);
    }
}
