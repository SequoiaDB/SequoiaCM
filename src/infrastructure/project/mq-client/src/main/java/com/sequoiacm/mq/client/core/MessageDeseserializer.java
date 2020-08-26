package com.sequoiacm.mq.client.core;

import org.bson.BSONObject;

public interface MessageDeseserializer<M> {
    public M deserialize(BSONObject m);
}
