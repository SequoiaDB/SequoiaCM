package com.sequoiacm.infrastructure.dispatcher.retry;

import com.netflix.client.config.IClientConfig;
import com.sequoiacm.infrastructure.common.ScmLoadBalancerUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScmRetryConfigFactory {

    private static final Map<String, ScmRetryConfig> retryConfigMap = new ConcurrentHashMap<>();

    public static ScmRetryConfig getRetryConfig(String service) {
        ScmRetryConfig scmRetryConfig = retryConfigMap.get(service);
        if (scmRetryConfig != null) {
            return scmRetryConfig;
        }
        IClientConfig loadBalancerConfig = ScmLoadBalancerUtil.getLoadBalancerConfig(service);
        ScmRetryConfig newScmRetryConfig = new ScmRetryConfig(loadBalancerConfig);
        retryConfigMap.put(service, newScmRetryConfig);
        return newScmRetryConfig;
    }
}
