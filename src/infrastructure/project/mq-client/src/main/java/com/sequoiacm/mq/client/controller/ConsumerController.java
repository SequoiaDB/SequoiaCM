package com.sequoiacm.mq.client.controller;

import org.springframework.web.bind.annotation.*;

import com.sequoiacm.mq.client.core.ConsumerClient;
import com.sequoiacm.mq.client.core.ConsumerClientMgr;
import com.sequoiacm.mq.core.CommonDefine;

@RequestMapping("/internal/v1")
@ResponseBody
public class ConsumerController {
    private final ConsumerClientMgr consumerClientMgr;

    public ConsumerController(ConsumerClientMgr consumerClientMgr) {
        this.consumerClientMgr = consumerClientMgr;
    }

    @GetMapping("/msg_queue/client/is_up")
    public boolean isUp(@RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group) {
        if (consumerClientMgr.getConsumerClient(group) != null) {
            return true;
        }
        return false;
    }

    @PostMapping("/msg_queue/client/new_msg_notify")
    public void newMsgNotify(@RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group) {
        ConsumerClient client = consumerClientMgr.getConsumerClient(group);
        if (client != null) {
            client.notifyNewMsgArrive();
        }
    }
}
