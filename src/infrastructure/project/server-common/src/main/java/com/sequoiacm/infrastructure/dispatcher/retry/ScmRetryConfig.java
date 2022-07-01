package com.sequoiacm.infrastructure.dispatcher.retry;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

public class ScmRetryConfig {

    private boolean okToRetryOnAllOperations = false;
    private int maxAutoRetries = 0;
    private int maxAutoRetriesNextServer = 2;

    public ScmRetryConfig(IClientConfig loadBalancerConfig) {
        this.maxAutoRetries = loadBalancerConfig.get(CommonClientConfigKey.MaxAutoRetries,
                DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES);
        this.maxAutoRetriesNextServer = loadBalancerConfig.get(
                CommonClientConfigKey.MaxAutoRetriesNextServer,
                DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER);
        this.okToRetryOnAllOperations = loadBalancerConfig
                .get(CommonClientConfigKey.OkToRetryOnAllOperations, false);
    }

    public boolean isOkToRetryOnAllOperations() {
        return okToRetryOnAllOperations;
    }

    public void setOkToRetryOnAllOperations(boolean okToRetryOnAllOperations) {
        this.okToRetryOnAllOperations = okToRetryOnAllOperations;
    }

    public int getMaxAutoRetries() {
        return maxAutoRetries;
    }

    public void setMaxAutoRetries(int maxAutoRetries) {
        this.maxAutoRetries = maxAutoRetries;
    }

    public int getMaxAutoRetriesNextServer() {
        return maxAutoRetriesNextServer;
    }

    public void setMaxAutoRetriesNextServer(int maxAutoRetriesNextServer) {
        this.maxAutoRetriesNextServer = maxAutoRetriesNextServer;
    }
}
