package com.sequoiacm.infrastructure.ribbon;

import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScmAlongGroupAccessServersFilter implements ScmServersFilter {

    private final ScmLoadBalancerRuleConfig config;

    public ScmAlongGroupAccessServersFilter(ScmLoadBalancerRuleConfig scmLoadBalancerRuleConfig) {
        this.config = scmLoadBalancerRuleConfig;
    }

    @Override
    public List<Server> doFilter(List<Server> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        List<Server> matchedServers = new ArrayList<>(servers.size());
        for (Server s : servers) {
            DiscoveryEnabledServer server = (DiscoveryEnabledServer) s;
            String remoteZone = server.getZone();
            if (Objects.equals(config.getZone(), remoteZone)) {
                String remoteNodeGroup = server.getInstanceInfo().getMetadata()
                        .get(ScmLoadBalancerRuleConfig.METADATA_KEY_NODE_GROUP);
                if (Objects.equals(config.getNodeGroup(), remoteNodeGroup)) {
                    matchedServers.add(server);
                }
            }
        }
        return matchedServers;
    }

    @Override
    public boolean shouldFilter() {
        return config.getNodeGroup() != null
                && ScmLoadBalancerRuleConfig.GROUP_ACCESS_MODE_ALONG
                        .equals(config.getGroupAccessMode());
    }
}
