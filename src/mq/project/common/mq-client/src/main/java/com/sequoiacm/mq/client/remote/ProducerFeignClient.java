package com.sequoiacm.mq.client.remote;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;

@RequestMapping("/internal/v1/msg_queue")
public interface ProducerFeignClient {
    @PostMapping(value = "/msg")
    public void putMsg(@RequestParam(CommonDefine.REST_TOPIC) String topic,
            @RequestParam(CommonDefine.REST_KEY) String key,
            @RequestParam(CommonDefine.REST_MSG_CONTENT) BSONObject content,
            @RequestParam(CommonDefine.REST_ACTION) String action) throws MqException;
}
