package com.sequoiacm.infrastructure.dispatcher.retry;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.retry.RetryContext;
import org.springframework.retry.context.RetryContextSupport;

public class ScmRetryContext extends RetryContextSupport {

    private ServiceInstance serviceInstance;

    public ScmRetryContext(RetryContext parent) {
        super(parent);
    }

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }
}
