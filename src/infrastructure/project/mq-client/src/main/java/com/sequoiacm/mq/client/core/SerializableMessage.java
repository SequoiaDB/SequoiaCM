package com.sequoiacm.mq.client.core;

import org.bson.BSONObject;

public interface SerializableMessage {
    BSONObject serialize();
}
