package com.sequoiacm.infrastructure.config.client;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

public interface ScmConfSubscriber {
    public String myServiceName();

    public String subscribeConfigName();

    public void processNotify(NotifyOption notification) throws Exception;

    public VersionFilter getVersionFilter();

    public long getHeartbeatIterval();

    default long getInitStatusInterval() {
        return 0;
    }

    public NotifyOption versionToNotifyOption(EventType eventType, Version version);

}
