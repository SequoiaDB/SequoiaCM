package com.sequoiacm.infrastructure.ribbon;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义负载均衡规则：优先选择同一台机器上的节点，如果同一台机器上没有，则退化成默认的：com.netflix.loadbalancer.ZoneAvoidanceRule
 * @see ZoneAvoidanceRule
 */
public class ScmLocalPreferredRule extends PredicateBasedRule {
    private static final Logger logger = LoggerFactory.getLogger(ScmLocalPreferredRule.class);

    private CompositePredicate compositePredicate;

    @Override
    public AbstractServerPredicate getPredicate() {
        return compositePredicate;
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        logger.info("Using ScmLocalPreferredRule");
        ScmLocalPreferredPredicate scmLocalPreferredPredicate = new ScmLocalPreferredPredicate(this, clientConfig);
        ZoneAvoidancePredicate zonePredicate = new ZoneAvoidancePredicate(this, clientConfig);
        AvailabilityPredicate availabilityPredicate = new AvailabilityPredicate(this, clientConfig);
        compositePredicate = createCompositePredicate(scmLocalPreferredPredicate, zonePredicate, availabilityPredicate);
    }

    private CompositePredicate createCompositePredicate(ScmLocalPreferredPredicate p1, ZoneAvoidancePredicate p2, AvailabilityPredicate p3) {
        return CompositePredicate.withPredicates(p1, p2, p3)
                .addFallbackPredicate(CompositePredicate.withPredicates(p2, p3).build())
                .addFallbackPredicate(p3)
                .addFallbackPredicate(AbstractServerPredicate.alwaysTrue())
                .build();
    }
}
