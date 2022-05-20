package com.sequoiacm.cloud.adminserver.common;

import com.netflix.appinfo.InstanceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class DiscoveryUtils {

    private final static Logger logger = LoggerFactory.getLogger(DiscoveryUtils.class);

    private static DiscoveryClient discoveryClient;

    public DiscoveryUtils(DiscoveryClient discoveryClient) {
        DiscoveryUtils.discoveryClient = discoveryClient;
    }

    public static InstanceInfo getInstanceByNodeUrl(String nodeURl) {
        String[] split = nodeURl.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        for (String service : discoveryClient.getServices()) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            for (ServiceInstance instance : instances) {
                EurekaServiceInstance eurekaInstance = (EurekaServiceInstance) instance;
                InstanceInfo instanceInfo = eurekaInstance.getInstanceInfo();
                if ((host.equals(instanceInfo.getIPAddr())
                        || host.equals(instanceInfo.getHostName()))
                        && port == instanceInfo.getPort()) {
                    return instanceInfo;
                }
            }
        }
        return null;
    }
}
