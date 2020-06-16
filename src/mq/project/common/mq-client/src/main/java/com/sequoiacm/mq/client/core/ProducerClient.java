package com.sequoiacm.mq.client.core;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqException;

public class ProducerClient<T> {
    private ProducerClientMgr mgr;
    private MessageSeserializer<T> s;
    private String topic;

    public ProducerClient(ProducerClientMgr mgr, String topic, MessageSeserializer<T> s) {
        this.mgr = mgr;
        this.topic = topic;
        this.s = s;
    }

    public void putMsg(String key, T messageContent) throws MqException {
        mgr.getClient().putMsg(topic, key, s.serialize(messageContent),
                CommonDefine.REST_ACTION_PUT);
    }

}
