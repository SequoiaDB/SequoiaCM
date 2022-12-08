package com.sequoiacm.infrastructure.ribbon;

import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScmAcrossGroupAccessServersFilter implements ScmServersFilter {

    private final ScmLoadBalancerRuleConfig config;

    public ScmAcrossGroupAccessServersFilter(ScmLoadBalancerRuleConfig scmLoadBalancerRuleConfig) {
        this.config = scmLoadBalancerRuleConfig;
    }

    @Override
    public List<Server> doFilter(List<Server> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        // 相同 zone 相同 group
        List<Server> matchedServers = null;

        // 相同 zone 不同 group
        List<Server> fallbackServers1 = null;

        // 不同 zone 相同 group
        List<Server> fallbackServers2 = null;

        // 不同 zone 不同 group
        List<Server> fallbackServers3 = null;
        for (Server s : servers) {
            DiscoveryEnabledServer server = (DiscoveryEnabledServer) s;
            String remoteZone = server.getZone();
            String remoteNodeGroup = server.getInstanceInfo().getMetadata()
                    .get(ScmLoadBalancerRuleConfig.METADATA_KEY_NODE_GROUP);
            // 在同一个 zone
            if (Objects.equals(config.getZone(), remoteZone)) {
                if (Objects.equals(config.getNodeGroup(), remoteNodeGroup)) {
                    matchedServers = ensureListAndAddValue(server, matchedServers);
                }
                else {
                    fallbackServers1 = ensureListAndAddValue(server, fallbackServers1);
                }
            }
            // 不在同一个 zone
            else {
                if (Objects.equals(config.getNodeGroup(), remoteNodeGroup)) {
                    fallbackServers2 = ensureListAndAddValue(server, fallbackServers2);
                }
                else {
                    fallbackServers3 = ensureListAndAddValue(server, fallbackServers3);
                }
            }
        }
        if (matchedServers != null) {
            return matchedServers;
        }
        if (fallbackServers1 != null) {
            return fallbackServers1;
        }
        if (fallbackServers2 != null) {
            return fallbackServers2;
        }
        return fallbackServers3;
    }

    private List<Server> ensureListAndAddValue(Server server, List<Server> servers) {
        if (servers == null) {
            servers = new ArrayList<>();
        }
        servers.add(server);
        return servers;
    }

    @Override
    public boolean shouldFilter() {
        return config.getNodeGroup() != null
                && ScmLoadBalancerRuleConfig.GROUP_ACCESS_MODE_ACROSS
                        .equals(config.getGroupAccessMode());
    }
}
