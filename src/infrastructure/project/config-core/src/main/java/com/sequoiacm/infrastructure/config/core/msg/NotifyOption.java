package com.sequoiacm.infrastructure.config.core.msg;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;

public interface NotifyOption {
    public EventType getEventType();

    public Version getVersion();

    public BSONObject toBSONObject();
}
