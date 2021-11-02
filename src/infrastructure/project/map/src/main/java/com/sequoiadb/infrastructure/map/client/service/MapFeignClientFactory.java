package com.sequoiadb.infrastructure.map.client.service;

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
public class MapFeignClientFactory {
    private final Map<String, MapFeignClient> nodeMapFeignClient = new ConcurrentHashMap<String, MapFeignClient>();
    private final Map<String, MapFeignClient> siteMapFeignClient = new ConcurrentHashMap<String, MapFeignClient>();
    private final MapFeignExceptionConverter exceptionConverter = new MapFeignExceptionConverter();

    @Autowired
    private ScmFeignClient scmFeignClient;

    private static ObjectMapper mapper = new ObjectMapper();
    static {
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
    }

    public MapFeignClient getFeignClientByServiceName(String nodeName) {
        if (siteMapFeignClient.containsKey(nodeName)) {
            return siteMapFeignClient.get(nodeName);
        }
        else {
            MapFeignClient client = scmFeignClient.builder().exceptionConverter(exceptionConverter)
                    .loggerLevel(Level.BASIC)
                    .objectMapper(mapper)
                    .serviceTarget(MapFeignClient.class, nodeName.toLowerCase());
            siteMapFeignClient.put(nodeName, client);
            return client;
        }
    }

    public MapFeignClient getFeignClientByNodeUrl(String nodeURl) {
        if (nodeMapFeignClient.containsKey(nodeURl)) {
            return nodeMapFeignClient.get(nodeURl);
        }
        else {
            MapFeignClient client = scmFeignClient.builder().exceptionConverter(exceptionConverter)
                    .loggerLevel(Level.BASIC)
                    .objectMapper(mapper).instanceTarget(MapFeignClient.class, nodeURl);
            nodeMapFeignClient.put(nodeURl, client);
            return client;
        }
    }
}
