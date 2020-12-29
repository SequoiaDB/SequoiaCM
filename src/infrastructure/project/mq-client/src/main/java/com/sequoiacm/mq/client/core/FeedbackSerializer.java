package com.sequoiacm.mq.client.core;

import org.bson.BSONObject;

public interface FeedbackSerializer<F> {
    public BSONObject serialize(F f);
}
