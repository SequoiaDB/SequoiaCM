package com.sequoiacm.config.framework.subscriber;

public interface ScmConfSubscriberFactory {
    public ScmConfSubscriber createSubscriber(String serviceName);
}
