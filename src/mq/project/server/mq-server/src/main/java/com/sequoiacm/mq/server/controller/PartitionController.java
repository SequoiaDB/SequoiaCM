package com.sequoiacm.mq.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.server.service.PartitionService;

@RequestMapping("/internal/v1/msg_queue")
@RestController
public class PartitionController {

    @Autowired
    private PartitionService service;

    @PostMapping(value = "/partition", params = CommonDefine.REST_ACTION + "="
            + CommonDefine.REST_ACQUIRE)
    public boolean acquiresPartition(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String consumerGroup,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer) throws MqException {
        return service.acquiresPartition(consumerGroup, consumer);
    }

    @PostMapping(value = "/partition", params = CommonDefine.REST_ACTION + "="
            + CommonDefine.REST_RELEASE)
    public void releasesPartition(
            @RequestParam(CommonDefine.REST_CONSUMER_GROUP) String consumerGroup,
            @RequestParam(CommonDefine.REST_CONSUMER) String consumer) throws MqException {
        service.releasePartition(consumerGroup, consumer);
    }
}
