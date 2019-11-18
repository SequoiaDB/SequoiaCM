package com.sequoiacm.config.framework.subscriber;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface ScmConfSubscriber {
    public boolean isNeedNotify(ScmConfEvent e) throws ScmConfigException;

    public String getConfigName();

    public String getServiceName();
}
