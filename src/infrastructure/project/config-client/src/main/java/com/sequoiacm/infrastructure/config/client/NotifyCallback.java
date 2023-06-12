package com.sequoiacm.infrastructure.config.client;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public interface NotifyCallback {

    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    void processNotify(EventType type, String businessName, NotifyOption notification)
            throws Exception;

    default int priority() {
        return LOWEST_PRECEDENCE;
    }
}
