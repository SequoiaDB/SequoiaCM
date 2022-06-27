package com.sequoiacm.infrastructure.ribbon;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerPredicate;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PredicateKey;

public class ScmLocalPreferredPredicate extends AbstractServerPredicate {

    private String localHostName = null;

    public ScmLocalPreferredPredicate(IRule rule, IClientConfig clientConfig) {
        super(rule, clientConfig);
        this.localHostName = clientConfig.get(ScmRibbonClientConfiguration.localHostName);
    }

    @Override
    public boolean apply(PredicateKey input) {
        if (localHostName == null || input == null || input.getServer() == null) {
            // should never happen
            return false;
        }
        return localHostName.equals(input.getServer().getHost());
    }
}
