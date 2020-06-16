package com.sequoiacm.mq.client.core;

import org.bson.BSONObject;

public interface MessageSeserializer<M> {
    public BSONObject serialize(M message);
}
