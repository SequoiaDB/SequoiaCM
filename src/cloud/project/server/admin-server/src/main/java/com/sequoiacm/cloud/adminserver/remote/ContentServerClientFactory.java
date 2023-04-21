package com.sequoiacm.cloud.adminserver.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.appinfo.InstanceInfo;
import com.sequoiacm.cloud.adminserver.common.DiscoveryUtils;
import com.sequoiacm.cloud.adminserver.common.MonitorDefine;
import com.sequoiacm.cloud.adminserver.model.ObjectDeltaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;

import feign.Request.Options;

@Component
public class ContentServerClientFactory {
    private static final Map<String, ContentServerClient> nodeMapFeignClient = new ConcurrentHashMap<String, ContentServerClient>();
    private static final Map<String, ContentServerClient> siteMapFeignClient = new ConcurrentHashMap<String, ContentServerClient>();
    private static final FileDeltaInfoDecoder fileDeltaDecoder = new FileDeltaInfoDecoder();
    private static final ObjectDeltaInfoDecoder objectDeltaInfoDecoder = new ObjectDeltaInfoDecoder();
    private static final ContentServerFeignExceptionConverter exceptionConverter = new ContentServerFeignExceptionConverter();

    private static ScmFeignClient feignClient;

    @Autowired
    public void setFeignClient(ScmFeignClient feignClient) {
        ContentServerClientFactory.feignClient = feignClient;
    }

    public static ContentServerClient getFeignClientByNodeUrl(String nodeURl) {
        InstanceInfo instance = DiscoveryUtils.getInstanceByNodeUrl(nodeURl);
        Class<? extends ContentServerClient> clazz = getContentServerClientClass(instance);
        if (nodeMapFeignClient.containsKey(nodeURl)) {
            ContentServerClient contentServerClient = nodeMapFeignClient.get(nodeURl);
            if (clazz != contentServerClient.getClass().getInterfaces()[0]) {
                nodeMapFeignClient.remove(nodeURl);
            }
            else {
                return contentServerClient;
            }
        }
        ContentServerClient client = feignClient.builder()
                .typeDecoder(FileDeltaInfo.class, fileDeltaDecoder)
                .typeDecoder(ObjectDeltaInfo.class, objectDeltaInfoDecoder)
                .exceptionConverter(exceptionConverter).instanceTarget(clazz, nodeURl);
        nodeMapFeignClient.put(nodeURl, client);
        return client;
    }

    private static Class<? extends ContentServerClient> getContentServerClientClass(
            InstanceInfo instance) {
        if (instance == null) {
            return ContentServerClient.class;
        }
        if (instance.getMetadata().containsKey(MonitorDefine.ACTUATOR_SECURITY_ENABLED)) {
            return ContentServerClientInternalMetrics.class;
        }
        else {
            return ContentServerClient.class;
        }
    }
}
