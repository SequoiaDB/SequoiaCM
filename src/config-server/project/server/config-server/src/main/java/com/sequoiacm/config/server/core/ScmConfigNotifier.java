package com.sequoiacm.config.server.core;

import java.util.List;

import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
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

    @Autowired
    ConfigEntityTranslator configEntityTranslator;

    public void notifyServices(List<ScmConfSubscriber> serviceList, ScmConfEvent event,
            boolean isAsyncNotify) {
        for (ScmConfSubscriber subscriber : serviceList) {
            notifyService(subscriber.getServiceName(), event, isAsyncNotify);
        }
    }

    private void notifyService(String service, ScmConfEvent event, boolean isAsyncNotify) {
        List<ServiceInstance> instances = discoveryClient.getInstances(service);
        if (instances == null || instances.size() <= 0) {
            logger.warn("notify failed,no instance for service: service={}, event={}", service,
                    event);
            return;
        }

        logger.info("notifying service: service={}, event={}", service, event);
        for (ServiceInstance instance : instances) {
            try {
                String url = instance.getHost() + ":" + instance.getPort();
                logger.info("notifying instance: instance={}", instance.getUri());
                ScmConfClient c = clientFactory.getClient(url);
                c.notifyInstance(event.getBusinessType(),
                        event.getEventType().toString(),
                        configEntityTranslator.toNotifyOptionBSON(event.getNotifyOption()),
                        isAsyncNotify);
            }
            catch (Exception e) {
                logger.warn("notify instance failed:service={}, instance={}, event={}", service,
                        instance.getUri(), event, e);
            }
        }
        logger.info("notify service done");

    }

}
