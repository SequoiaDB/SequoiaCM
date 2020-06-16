package com.sequoiacm.mq.client.controller;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequoiacm.mq.core.CommonDefine;

@RequestMapping("/internal/v1")
@ResponseBody
public class ConsumerController {
    private Queue<String> groups = new ConcurrentLinkedQueue<String>();

    @GetMapping("/msg_queue/client/is_up")
    public boolean isUp(@RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group) {
        if (groups.contains(group)) {
            return true;
        }
        return false;
    }

    public void consumerDown(String consumerGroup) {
        groups.remove(consumerGroup);
    }

    public void consumerUp(String consumerGroup) {
        groups.add(consumerGroup);
    }
}
