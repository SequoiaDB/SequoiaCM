package com.sequoiacm.mq.client.core;

import org.bson.BSONObject;

public interface MessageDeserializer<M> {
    public M deserialize(BSONObject m);
}
