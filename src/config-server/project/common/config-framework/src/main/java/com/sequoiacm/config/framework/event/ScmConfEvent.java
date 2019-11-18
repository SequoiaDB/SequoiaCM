package com.sequoiacm.config.framework.event;

import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public interface ScmConfEvent {
    String getConfigName();

    NotifyOption getNotifyOption();
}
