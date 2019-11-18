package com.sequoiacm.cloud.adminserver.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final ContentServerFeignExceptionConverter exceptionConverter = new ContentServerFeignExceptionConverter();

    private static ScmFeignClient feignClient;

    @Autowired
    public void setFeignClient(ScmFeignClient feignClient) {
        ContentServerClientFactory.feignClient = feignClient;
    }

    public static ContentServerClient getFeignClientByServiceName(String name) {
        if (siteMapFeignClient.containsKey(name)) {
            return siteMapFeignClient.get(name);
        } else {
            ContentServerClient client = feignClient.builder()
                    .typeDecoder(FileDeltaInfo.class, fileDeltaDecoder)
                    .exceptionConverter(exceptionConverter)
                    .options(new Options(30 * 1000, 600 * 1000))
                    .serviceTarget(ContentServerClient.class, name.toLowerCase());
            siteMapFeignClient.put(name, client);
            return client;
        }
    }

    public static ContentServerClient getFeignClientByNodeUrl(String nodeURl) {
        if (nodeMapFeignClient.containsKey(nodeURl)) {
            return nodeMapFeignClient.get(nodeURl);
        } else {
            ContentServerClient client = feignClient.builder()
                    .typeDecoder(FileDeltaInfo.class, fileDeltaDecoder)
                    .exceptionConverter(exceptionConverter)
                    .options(new Options(30 * 1000, 600 * 1000))
                    .instanceTarget(ContentServerClient.class, nodeURl);
            nodeMapFeignClient.put(nodeURl, client);
            return client;
        }
    }
}
