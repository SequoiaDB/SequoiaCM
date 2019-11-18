package com.sequoiacm.config.framework;

import com.sequoiacm.config.framework.subscriber.ScmConfSubscriberFactory;
import com.sequoiacm.config.framework.subscriber.ScmDefaultConfSubscriberFactory;

public abstract class ScmConfFrameworkBase implements ScmConfFramework {

    private String configName;

    public ScmConfFrameworkBase(String configName) {
        this.configName = configName;
    }

    @Override
    public ScmConfSubscriberFactory getSubscriberFactory() {
        return new ScmDefaultConfSubscriberFactory(configName);
    }

    @Override
    public String getConfigName() {
        return configName;
    }

}
