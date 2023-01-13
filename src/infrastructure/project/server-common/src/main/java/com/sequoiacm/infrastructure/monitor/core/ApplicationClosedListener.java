package com.sequoiacm.infrastructure.monitor.core;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

// listen to scm nodes which in service-center
public class ApplicationClosedListener implements ApplicationListener<ContextClosedEvent> {

    private Logger logger = LoggerFactory.getLogger(ApplicationClosedListener.class);

    private static final String SERVICE_CENTER_NAME = "service-center";

    private static final String REST_PATH = "/internal/v1/instances";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private EurekaInstanceConfigBean eurekaInstanceConfigBean;

    private boolean closed = false;

    @Autowired
    ScmFeignClient scmFeignClient;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (closed) {
            return;
        }
        closed = true;
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_CENTER_NAME);
        if (instances.size() == 0) {
            logger.warn(
                    "instance status was not updated from service-center because there are no service-center nodes.");
            return;
        }
        // 这里不建议使用 feignClient 发送请求：
        // 1. 重试时会创建多个 feignClient，浪费资源
        // 2. 仅是发简单请求，不需要用到 feignClient 的内容编码、熔断等特性
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        Exception lastException = null;
        boolean success = false;
        for (ServiceInstance instance : instances) {
            String url = "http://" + instance.getHost() + ":" + instance.getPort() + REST_PATH
                    + "?action=stop&ip_addr=" + eurekaInstanceConfigBean.getIpAddress() + "&port="
                    + eurekaInstanceConfigBean.getNonSecurePort();
            try {
                restTemplate.put(url, null);
            }
            catch (Exception e) {
                lastException = e;
                continue;
            }
            success = true;
            break;
        }
        if (!success) {
            logger.warn("failed to notify service-center to update instance status.",
                    lastException);
        }
        else {
            logger.info("successfully notify service-center to update instance status.");
        }

    }
}
