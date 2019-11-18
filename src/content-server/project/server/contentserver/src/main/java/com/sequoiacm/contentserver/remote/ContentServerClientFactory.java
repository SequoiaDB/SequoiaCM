package com.sequoiacm.contentserver.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;

import feign.Logger.Level;
import feign.Request.Options;

@Component
public class ContentServerClientFactory {
    private static final Map<String, ContentServerClient> nodeMapFeignClient = new ConcurrentHashMap<String, ContentServerClient>();
    private static final Map<String, ContentServerClient> siteMapFeignClient = new ConcurrentHashMap<String, ContentServerClient>();
    private static final DataInfoDecoder dataInfoDecoder = new DataInfoDecoder();
    private static final ContentServerFeignExceptionConverter exceptionConverter = new ContentServerFeignExceptionConverter();

    private static ScmFeignClient scmFeignClient;

    private static ObjectMapper mapper = new ObjectMapper();
    static {
        SimpleModule module = new SimpleModule();
//        module.addDeserializer();
        mapper.registerModule(module);
    }

    @Autowired
    public void setFeignClient(ScmFeignClient feignClient) {
        ContentServerClientFactory.scmFeignClient = feignClient;
    }

    public static ContentServerClient getFeignClientByServiceName(String name) {
        if (siteMapFeignClient.containsKey(name)) {
            return siteMapFeignClient.get(name);
        }
        else {
            ContentServerClient client = scmFeignClient.builder()
                    .typeDecoder(DataInfo.class, dataInfoDecoder)
                    .exceptionConverter(exceptionConverter)
                    .options(new Options(30 * 1000, 600 * 1000)).loggerLevel(Level.BASIC)
                    .objectMapper(mapper)
                    .serviceTarget(ContentServerClient.class, name.toLowerCase());
            siteMapFeignClient.put(name, client);
            return client;
        }
    }

    public static ContentServerClient getFeignClientByNodeUrl(String nodeURl) {
        if (nodeMapFeignClient.containsKey(nodeURl)) {
            return nodeMapFeignClient.get(nodeURl);
        }
        else {
            ContentServerClient client = scmFeignClient.builder()
                    .typeDecoder(DataInfo.class, dataInfoDecoder)
                    .exceptionConverter(exceptionConverter).loggerLevel(Level.BASIC)
                    .options(new Options(30 * 1000, 600 * 1000)).objectMapper(mapper)
                    .instanceTarget(ContentServerClient.class, nodeURl);
            nodeMapFeignClient.put(nodeURl, client);
            return client;
        }
    }
}
