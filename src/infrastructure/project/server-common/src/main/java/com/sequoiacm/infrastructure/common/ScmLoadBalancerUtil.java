package com.sequoiacm.infrastructure.common;

import com.google.common.collect.Lists;
import com.netflix.client.Utils;
import com.netflix.loadbalancer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.stereotype.Component;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

@Component
public class ScmLoadBalancerUtil {

    private static SpringClientFactory clientFactory;

    private static final List<Class<? extends Throwable>> circuitRelated = Lists
            .<Class<? extends Throwable>> newArrayList(SocketException.class,
                    SocketTimeoutException.class);

    public static void recordInstanceError(ServiceInstance instance, Throwable e) {
        if (Utils.isPresentAsCause(e, circuitRelated)) {
            ServerStats serverStats = getServerStats(instance);
            if (serverStats != null) {
                serverStats.incrementSuccessiveConnectionFailureCount();
                serverStats.addToFailureCount();
            }
        }
    }

    public static void resetInstanceError(ServiceInstance instance) {
        ServerStats serverStats = getServerStats(instance);
        if (serverStats != null) {
            serverStats.clearSuccessiveConnectionFailureCount();
        }
    }

    private static ServerStats getServerStats(ServiceInstance instance) {
        if (instance instanceof RibbonLoadBalancerClient.RibbonServer) {
            Server lbServer = ((RibbonLoadBalancerClient.RibbonServer) instance).getServer();
            ILoadBalancer loadBalancer = clientFactory.getLoadBalancer(instance.getServiceId());
            if (loadBalancer instanceof AbstractLoadBalancer) {
                LoadBalancerStats lbStats = ((AbstractLoadBalancer) loadBalancer)
                        .getLoadBalancerStats();
                return lbStats.getSingleServerStat(lbServer);
            }
        }
        return null;
    }

    @Autowired
    public void setClientFactory(SpringClientFactory clientFactory) {
        ScmLoadBalancerUtil.clientFactory = clientFactory;
    }
}
