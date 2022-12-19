package com.sequoiacm.cloud.servicetrace;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.monitor.config.EnableScmMonitorServer;

import zipkin.server.EnableZipkinServer;

@EnableDiscoveryClient
@EnableZipkinServer
@SpringBootApplication
@EnableScmMonitorServer
@EnableConfClient
public class ServiceTrace implements ApplicationRunner {

    @Autowired
    ScmConfClient confClient;

    @Autowired
    DiscoveryClient discoveryClient;

    @Autowired
    private EurekaInstanceConfigBean instanceConfigBean;

    public static void main(String[] args) {
        new SpringApplicationBuilder(ServiceTrace.class).bannerMode(Banner.Mode.OFF).run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        checkServiceNumLimit();
    }

    private void checkServiceNumLimit() {
        List<ServiceInstance> instances = discoveryClient.getInstances("service-trace");
        if (instances.size() == 0) {
            return;
        }
        if (instances.size() > 1) {
            throw new RuntimeException(
                    "The number of service-trace nodes exceeds the limit num: 1, current num: "
                            + instances.size() + ", instances=" + instances);
        }
        else {
            ServiceInstance instance = instances.get(0);
            if (!(instance.getHost().equals(instanceConfigBean.getHostname())
                    && instance.getPort() == instanceConfigBean.getNonSecurePort())) {
                throw new RuntimeException(
                        "The number of service-trace nodes exceeds the limit num: 1");
            }
        }

    }

}
