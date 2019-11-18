package com.sequoiacm.config.server.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.server.remote.ScmConfClient;
import com.sequoiacm.config.server.remote.ScmConfClientFactory;

@Component
public class ScmConfigNotifier {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfigNotifier.class);
    @Autowired
    DiscoveryClient discoveryClient;
    @Autowired
    ScmConfClientFactory clientFactory;

    public void notifyServices(List<String> serviceList, ScmConfEvent event,
            boolean isAsyncNotify) {
        for (String service : serviceList) {
            notifyService(service, event, isAsyncNotify);
        }
    }

    private void notifyService(String service, ScmConfEvent event, boolean isAsyncNotify) {
        List<ServiceInstance> instances = discoveryClient.getInstances(service);
        if (instances == null || instances.size() <= 0) {
            logger.warn("notify failed,no instance for service: service={}, event={}", service,
                    event);
            return;
        }

        for (ServiceInstance instance : instances) {
            try {
                String url = instance.getHost() + ":" + instance.getPort();
                ScmConfClient c = clientFactory.getClient(url);
                c.notifyInstance(event.getConfigName(),
                        event.getNotifyOption().getEventType().toString(),
                        event.getNotifyOption().toBSONObject(), isAsyncNotify);
            }
            catch (Exception e) {
                logger.warn("notify instance failed:service={}, instance={}, event={}", service,
                        instance.getUri(), event, e);
            }
        }

    }

}
