package com.sequoiacm.mq.client.core;

import com.sequoiacm.mq.client.remote.ProducerFeignClient;
import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;

public class ProducerClient {
    private ProducerFeignClient client;

    public ProducerClient(ProducerFeignClient client) {
        this.client = client;
    }

    public long putMsg(String topic, String key, SerializeableMessage m) throws MqException {
        return client.putMsg(topic, key, m.serialize(), CommonDefine.REST_ACTION_PUT);
    }

}
