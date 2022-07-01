package com.sequoiacm.infrastructure.dispatcher.retry;

import com.sequoiacm.infrastructure.common.ScmLoadBalancerUtil;
import org.apache.http.HttpRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicy;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;

/**
 * 重试策略，参考 RibbonLoadBalancedRetryPolicy 实现
 * @see RibbonLoadBalancedRetryPolicy
 */
public class ScmRetryPolicy implements RetryPolicy {

    private final ServiceInstance initialServiceInstance;
    private final HttpRequest httpRequest;
    private int sameServerCount = 0;
    private int nextServerCount = 0;
    
    private boolean canRetryRequest = false;

   
    private final ScmRetryConfig retryConfig;

    public ScmRetryPolicy(HttpRequest httpRequest, ServiceInstance initialServiceInstance) {
        this.httpRequest = httpRequest;
        this.initialServiceInstance = initialServiceInstance;
        this.retryConfig = ScmRetryConfigFactory
                .getRetryConfig(initialServiceInstance.getServiceId());
        this.canRetryRequest = "GET".equalsIgnoreCase(httpRequest.getRequestLine().getMethod())
                || retryConfig.isOkToRetryOnAllOperations();
    }

    @Override
    public boolean canRetry(RetryContext context) {
        ScmRetryContext scmRetryContext = (ScmRetryContext) context;
        if (scmRetryContext.getRetryCount() == 0 && scmRetryContext.getServiceInstance() == null) {
            scmRetryContext.setServiceInstance(initialServiceInstance);
            return true;
        }
        return canRetryNextServer();
    }

    public boolean canRetryNextServer() {
        // this will be called after a failure occurs and we increment the counter
        // so we check that the count is less than or equals to too make sure
        // we try the next server the right number of times
        return nextServerCount <= retryConfig.getMaxAutoRetriesNextServer()
                && canRetryRequest;
    }
    
    @Override
    public RetryContext open(RetryContext parent) {
        return new ScmRetryContext(parent);
    }

    @Override
    public void close(RetryContext context) {

    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        ScmRetryContext scmRetryContext = (ScmRetryContext) context;
        scmRetryContext.registerThrowable(throwable);

        if (sameServerCount >= retryConfig.getMaxAutoRetries() && canRetryRequest) {
            // reset same server since we are moving to a new server
            sameServerCount = 0;
            nextServerCount++;
            if (canRetryNextServer()) {
                scmRetryContext.setServiceInstance(
                        ScmLoadBalancerUtil.chooseInstance(initialServiceInstance.getServiceId()));
            }
            else {
                context.setExhaustedOnly();
            }
        }
        else {
            sameServerCount++;
        }
    }
}
