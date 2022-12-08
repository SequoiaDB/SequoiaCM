package com.sequoiacm.infrastructure.ribbon;

import com.netflix.loadbalancer.Server;

import java.util.ArrayList;
import java.util.List;

public class ScmHostNameMatchServersFilter implements ScmServersFilter {

    private ScmLoadBalancerRuleConfig config;

    public ScmHostNameMatchServersFilter(ScmLoadBalancerRuleConfig scmLoadBalancerRuleConfig) {
        if (scmLoadBalancerRuleConfig.getLocalHostName() == null) {
            throw new IllegalArgumentException("localHostName is null");
        }
        this.config = scmLoadBalancerRuleConfig;
    }

    @Override
    public List<Server> doFilter(List<Server> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        List<Server> matchedServers = new ArrayList<>(servers.size());
        List<Server> fallbackServers = new ArrayList<>(servers.size());

        for (Server server : servers) {
            if (server.getHost().equals(config.getLocalHostName())) {
                matchedServers.add(server);
            }
            else {
                fallbackServers.add(server);
            }
        }
        if (!matchedServers.isEmpty()) {
            return matchedServers;
        }
        return fallbackServers;
    }

    @Override
    public boolean shouldFilter() {
        return config.isLocalPreferred();
    }
}
