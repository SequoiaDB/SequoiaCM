package com.sequoiacm.infrastructure.discovery;

import com.sequoiacm.infrastructure.common.NetUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.*;

public class ScmServiceDiscoveryClient {
    private final DiscoveryClient discoveryClient;

    @Value("${eureka.instance.metadata-map.zone:#{null}}")
    private String localZone;

    @Value("${eureka.instance.metadata-map.region:#{null}}")
    private String localRegion;

    public ScmServiceDiscoveryClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public String getLocalRegion() {
        return localRegion;
    }

    public String getLocalZone() {
        return localZone;
    }

    public Map<String, Set<String>> getZones() {
        List<String> allService = discoveryClient.getServices();
        Map<String, Set<String>> ret = new HashMap<>();
        for (String s : allService) {
            List<ScmServiceInstance> instances = getInstances(s);
            for (ScmServiceInstance instance : instances) {
                Set<String> sameRegionZones = ret.get(instance.getRegion());
                if (sameRegionZones == null) {
                    sameRegionZones = new HashSet<>();
                    ret.put(instance.getRegion(), sameRegionZones);
                }
                sameRegionZones.add(instance.getZone());
            }
        }
        return ret;
    }

    public List<ScmServiceInstance> getInstances() {
        List<String> services = discoveryClient.getServices();
        List<ScmServiceInstance> ret = new ArrayList<>();
        for (String service : services) {
            ret.addAll(getInstances(service));
        }
        return ret;
    }

    public List<ScmServiceInstance> getInstances(String service) {
        List<ServiceInstance> instances = discoveryClient.getInstances(service.toLowerCase());
        if (instances == null || instances.size() <= 0) {
            return Collections.emptyList();
        }
        List<ScmServiceInstance> ret = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            Map<String, String> meta = instance.getMetadata();
            ret.add(new ScmServiceInstance(instance.getHost(), instance.getPort(),
                    meta.get("region"), meta.get("zone"), instance.getMetadata(), service));
        }
        return ret;
    }

    public List<ScmServiceInstance> getInstances(String region, String service) {
        if (region == null) {
            return Collections.emptyList();
        }
        List<ScmServiceInstance> instances = getInstances(service);
        if (instances.size() <= 0) {
            return Collections.emptyList();
        }
        List<ScmServiceInstance> sameRegionInstances = new ArrayList<>();
        for (ScmServiceInstance instance : instances) {
            if (region.equals(instance.getRegion())) {
                sameRegionInstances.add(instance);
            }
        }
        return sameRegionInstances;
    }

    public List<ScmServiceInstance> getInstances(String region, String zone, String service) {
        if (region == null || zone == null) {
            return Collections.emptyList();
        }
        List<ScmServiceInstance> sameRegionInstances = getInstances(region, service);
        List<ScmServiceInstance> sameZoneInstances = new ArrayList<>();
        for (ScmServiceInstance instance : sameRegionInstances) {
            if (zone.equals(instance.getZone())) {
                sameZoneInstances.add(instance);
            }
        }
        return sameZoneInstances;
    }

    public ScmServiceInstance choseInstance(String region, String zone, String service) {
        Random random = new Random();
        List<ScmServiceInstance> instances = getInstances(region, zone, service);
        if (instances.size() > 0) {
            return instances.get(random.nextInt(instances.size()));
        }

        instances = getInstances(region, service);
        if (instances.size() > 0) {
            return instances.get(random.nextInt(instances.size()));
        }

        List<ScmServiceInstance> allInstances = getInstances(service);
        if (allInstances.size() > 0) {
            return allInstances.get(random.nextInt(allInstances.size()));
        }
        return null;
    }

    public String getServiceNameByUrl(String url) {
        String hostAndPort = NetUtil.getHostAndPort(url);
        List<String> services = discoveryClient.getServices();
        for (String service : services) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            for (ServiceInstance instance : instances) {
                String serviceUrl = instance.getHost() + ":" + instance.getPort();
                if (serviceUrl.equalsIgnoreCase(hostAndPort)) {
                    return service.toLowerCase();
                }
            }
        }
        return null;
    }
}
