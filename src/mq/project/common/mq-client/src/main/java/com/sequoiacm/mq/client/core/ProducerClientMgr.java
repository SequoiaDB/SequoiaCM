package com.sequoiacm.mq.client.core;

import com.sequoiacm.mq.client.remote.ProducerFeignClient;

public class ProducerClientMgr {
    private ProducerFeignClient client;

    public ProducerClientMgr(ProducerFeignClient client) {
        this.client = client;
    }

    public <T> ProducerClient<T> createClient(String topic, MessageSeserializer<T> s) {
        return new ProducerClient<>(this, topic, s);
    }

    ProducerFeignClient getClient() {
        return client;
    }
}
