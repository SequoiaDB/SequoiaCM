package com.sequoiacm.infrastructure.config.client.service;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface ScmConfigNotifyService {
    public void notify(String configName, EventType type, BSONObject notifyOption,
            boolean isAsyncNotify) throws ScmConfigException;
}
