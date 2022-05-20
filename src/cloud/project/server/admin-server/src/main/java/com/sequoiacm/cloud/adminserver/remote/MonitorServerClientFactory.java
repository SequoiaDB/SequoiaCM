package com.sequoiacm.cloud.adminserver.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.appinfo.InstanceInfo;
import com.sequoiacm.cloud.adminserver.common.DiscoveryUtils;
import com.sequoiacm.cloud.adminserver.common.MonitorDefine;
import com.sequoiacm.cloud.adminserver.model.HealthInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;

import feign.Request.Options;

@Component
public class MonitorServerClientFactory {
    private static final Map<String, MonitorServerClient> nodeMapFeignClient = new ConcurrentHashMap<String, MonitorServerClient>();
    private static final HealthInfoDecoder healthInfoDecoder = new HealthInfoDecoder();

    private static ScmFeignClient feignClient;

    @Autowired
    public void setFeignClient(ScmFeignClient feignClient) {
        MonitorServerClientFactory.feignClient = feignClient;
    }

    public static MonitorServerClient getFeignClientByNodeUrl(String nodeURl) {
        InstanceInfo instance = DiscoveryUtils.getInstanceByNodeUrl(nodeURl);
        Class<? extends MonitorServerClient> clazz = getMonitorClientClass(instance);
        if (nodeMapFeignClient.containsKey(nodeURl)) {
            MonitorServerClient monitorServerClient = nodeMapFeignClient.get(nodeURl);
            if (clazz != monitorServerClient.getClass().getInterfaces()[0]) {
                nodeMapFeignClient.remove(nodeURl);
            }
            else {
                return monitorServerClient;
            }
        }
        MonitorServerClient client = feignClient.builder()
                .typeDecoder(HealthInfo.class, healthInfoDecoder).instanceTarget(clazz, nodeURl);
        nodeMapFeignClient.put(nodeURl, client);
        return client;
    }

    private static Class<? extends MonitorServerClient> getMonitorClientClass(
            InstanceInfo instance) {
        if (instance == null) {
            return MonitorServerClient.class;
        }
        if (MonitorServerClientS3Health.S3_SERVER_NAME.equalsIgnoreCase(instance.getAppName())
                || instance.getHealthCheckUrl()
                .endsWith(MonitorServerClientS3Health.S3_HEALTH_PATH)) {
            return MonitorServerClientS3Health.class;
        }
        if (instance.getMetadata().containsKey(MonitorDefine.ACTUATOR_SECURITY_ENABLED)) {
            return MonitorServerClientInternalHealth.class;
        }
        else {
            return MonitorServerClient.class;
        }
    }
}
