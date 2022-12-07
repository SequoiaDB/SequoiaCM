package com.sequoiacm.infrastructure.config;

import com.netflix.appinfo.InstanceInfo;
import com.sequoiacm.infrastructure.common.ConditionalOnDiscoveryClient;
import com.sequoiacm.infrastructure.common.ScmCheckConnResult;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.remote.ConnectivityCheckHealth;
import feign.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 为SCM各节点提供与其他节点的连通性检查
 */
@Configuration
@ConditionalOnDiscoveryClient
public class ConnectivityCheckAutoConfiguration {
    private static final Logger logger = LoggerFactory
            .getLogger(ConnectivityCheckAutoConfiguration.class);

    @Bean
    public ConnectivityController getCheckConnResult() {
        return new ConnectivityController();
    }

    @Bean
    public CheckConnUtils doCheckConnUtils() {
        return new CheckConnUtils();
    }

    @ResponseBody
    @RequestMapping
    class ConnectivityController {

        @Autowired
        private CheckConnUtils checkConnUtils;

        @PostMapping("/internal/v1/connectivity-check")
        public List<ScmCheckConnResult> getCheckConnResult(
                @RequestParam(value = "nodes", required = false) String nodes,
                @RequestParam(value = "services", required = false) String services) {

            return checkConnUtils.getCheckConnResult(nodes, services);
        }
    }

    class CheckConnUtils {

        @Autowired
        private DiscoveryClient discoveryClient;

        @Autowired
        private ScmFeignClient feignClient;

        public List<ScmCheckConnResult> getCheckConnResult(String nodes, String services) {
            Set<ServiceInstance> checkInstances = prepareServiceInstance(nodes, services);
            List<ScmCheckConnResult> results = new ArrayList<>();
            for (ServiceInstance instance : checkInstances) {
                EurekaDiscoveryClient.EurekaServiceInstance eurekaServiceInstance = (EurekaDiscoveryClient.EurekaServiceInstance) instance;
                boolean checkConn = doCheckConn(eurekaServiceInstance);
                Map<String, String> metadata = instance.getMetadata();
                ScmCheckConnResult result = new ScmCheckConnResult(instance.getServiceId(),
                        eurekaServiceInstance.getInstanceInfo().getIPAddr(), instance.getHost(),
                        instance.getPort(), checkConn,metadata.get("region"), metadata.get("zone"));
                results.add(result);
            }
            return results;
        }

        private Set<ServiceInstance> prepareServiceInstance(String nodes, String services) {

            Set<ServiceInstance> checkInstances = new HashSet<>();

            HashSet<String> nodeSet = new HashSet<>();
            HashSet<String> serviceSet = new HashSet<>();
            if (!StringUtils.isEmpty(nodes)) {
                nodeSet.addAll(Arrays.asList(nodes.split(",")));
            }
            if (!StringUtils.isEmpty(services)) {
                serviceSet.addAll(Arrays.asList(services.toLowerCase().split(",")));
            }

            // get instance from EurekaServiceCenter
            List<String> serviceNames = discoveryClient.getServices();
            for (String serviceName : serviceNames) {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);

                if (StringUtils.isEmpty(nodes) && StringUtils.isEmpty(services)) {
                    checkInstances.addAll(instances);
                    continue;
                }

                if (!StringUtils.isEmpty(nodes)) {
                    for (ServiceInstance instance : instances) {
                        EurekaDiscoveryClient.EurekaServiceInstance eurekaInstance = (EurekaDiscoveryClient.EurekaServiceInstance) instance;
                        InstanceInfo instanceInfo = eurekaInstance.getInstanceInfo();
                        if (nodeSet.contains(instance.getHost() + ":" + instance.getPort())
                                || nodeSet.contains(
                                        instanceInfo.getIPAddr() + ":" + instance.getPort())) {
                            checkInstances.add(instance);
                            nodeSet.remove(instance.getHost() + ":" + instance.getPort());
                            nodeSet.remove(instanceInfo.getIPAddr() + ":" + instance.getPort());
                        }
                    }
                }

                if (!StringUtils.isEmpty(services)) {
                    if (serviceSet.contains(serviceName.toLowerCase())) {
                        checkInstances.addAll(instances);
                        serviceSet.remove(serviceName.toLowerCase());
                    }
                }
            }

            if (nodeSet.size() != 0) {
                throw new IllegalArgumentException("node not find," + "nodes is " + nodes);
            }
            if (serviceSet.size() != 0) {
                throw new IllegalArgumentException(
                        "service not find," + "serviceName = " + services);
            }
            return checkInstances;
        }

        public boolean doCheckConn(EurekaDiscoveryClient.EurekaServiceInstance instance) {

            ConnectivityCheckHealth client = feignClient.builder()
                    .options(new Request.Options(1000, 2000))
                    .instanceTarget(ConnectivityCheckHealth.class,
                            instance.getInstanceInfo().getIPAddr() + ":" + instance.getPort());
            Map<?, ?> result = null;
            try {
                if ("true".equals(instance.getMetadata().get("isS3Server"))) {
                    result = client.getS3Health();
                }
                else {
                    result = client.getHealth();
                }
                logger.debug("check conn result:" + result);
            }
            catch (Exception exception) {
                logger.warn("check connectivity failed,target service is ( "
                        + instance.getInstanceInfo().getIPAddr() + instance.getPort() + " )");
                return false;
            }
            if (!"UP".equalsIgnoreCase(String.valueOf(result.get("status")))) {
                return false;
            }
            return true;
        }

    }
}