package com.sequoiacm.config.framework.subscriber;

public class ScmDefaultConfSubscriberFactory implements ScmConfSubscriberFactory {
    private String configName;

    public ScmDefaultConfSubscriberFactory(String configName) {
        this.configName = configName;
    }

    @Override
    public ScmConfSubscriber createSubscriber(String serviceName) {
        return new ScmDefaultConfSubscriber(configName, serviceName);
    }

}
