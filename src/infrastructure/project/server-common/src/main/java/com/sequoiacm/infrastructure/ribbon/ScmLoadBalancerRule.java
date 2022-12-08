package com.sequoiacm.infrastructure.ribbon;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScmLoadBalancerRule extends AbstractLoadBalancerRule {

    private static final Logger logger = LoggerFactory.getLogger(ScmLoadBalancerRule.class);

    private ZoneAvoidanceRule zoneAvoidanceRule;

    private List<ScmServersFilter> scmServersFilters = Collections.emptyList();

    private final AtomicInteger nextIndex = new AtomicInteger();

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        ZoneAvoidanceRule zoneAvoidanceRule = new ZoneAvoidanceRule();
        zoneAvoidanceRule.initWithNiwsConfig(clientConfig);
        this.zoneAvoidanceRule = zoneAvoidanceRule;
        this.scmServersFilters = clientConfig
                .get(ScmRibbonClientConfiguration.scmServersFilterListKey);
        if (this.scmServersFilters == null) {
            this.scmServersFilters = Collections.emptyList();
        }
    }

    @Override
    public Server choose(Object key) {
        // 先走官方原生的负载均衡策略, 选择出一批节点
        List<Server> servers = getServers(key);
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        for (ScmServersFilter scmServersFilter : scmServersFilters) {
            if (!scmServersFilter.shouldFilter()) {
                continue;
            }
            servers = scmServersFilter.doFilter(servers);
            if (servers == null || servers.isEmpty()) {
                logger.warn("no available node for service:{}, lastServersFilter={}", key,
                        scmServersFilter);
                return null;
            }
        }

        return servers.get(incrementAndGetModulo(servers.size()));
    }

    private List<Server> getServers(Object key) {
        List<Server> servers = getLoadBalancer().getAllServers();
        if (servers.size() <= 0) {
            return null;
        }
        return zoneAvoidanceRule.getPredicate().getEligibleServers(servers, key);
    }

    /**
     * 参考：
     * @see RoundRobinRule#choose(ILoadBalancer, Object)
     */
    private int incrementAndGetModulo(int modulo) {
        for (;;) {
            int current = nextIndex.get();
            int next = (current + 1) % modulo;
            if (nextIndex.compareAndSet(current, next) && current < modulo)
                return current;
        }
    }
}
