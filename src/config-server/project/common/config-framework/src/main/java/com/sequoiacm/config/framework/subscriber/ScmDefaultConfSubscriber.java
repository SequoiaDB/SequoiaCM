package com.sequoiacm.config.framework.subscriber;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public class ScmDefaultConfSubscriber implements ScmConfSubscriber {

    private String configName;
    private String serviceName;

    public ScmDefaultConfSubscriber(String configName, String serviceName) {
        this.configName = configName;
        this.serviceName = serviceName;
    }

    @Override
    public boolean isNeedNotify(ScmConfEvent e) throws ScmConfigException {
        return true;
    }

    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

}
