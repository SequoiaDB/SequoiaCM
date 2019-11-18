package com.sequoiacm.cloud.adminserver.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.adminserver.model.HealthInfo;
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
        if (nodeMapFeignClient.containsKey(nodeURl)) {
            return nodeMapFeignClient.get(nodeURl);
        }
        else {
            MonitorServerClient client = feignClient.builder()
                    .typeDecoder(HealthInfo.class, healthInfoDecoder)
                    .options(new Options(30 * 1000, 600 * 1000))
                    .instanceTarget(MonitorServerClient.class, nodeURl);
            nodeMapFeignClient.put(nodeURl, client);
            return client;
        }
    }
}
