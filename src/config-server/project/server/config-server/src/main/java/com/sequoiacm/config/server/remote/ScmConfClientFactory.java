package com.sequoiacm.config.server.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;

import feign.Request.Options;

@Component
public class ScmConfClientFactory {
    @Autowired
    private ScmFeignClient scmFeignClient;

    private Map<String, ScmConfClient> urlMapClient = new ConcurrentHashMap<>();
    private Map<String, ScmConfClient> serviceMapClient = new ConcurrentHashMap<>();
    private ScmConfServerExceptionConvertor exceptionConvertor = new ScmConfServerExceptionConvertor();

    public ScmConfClient getClient(String nodeUrl) {
        if (urlMapClient.containsKey(nodeUrl)) {
            return urlMapClient.get(nodeUrl);
        }

        ScmConfClient client = scmFeignClient.builder().exceptionConverter(exceptionConvertor)
                .instanceTarget(ScmConfClient.class, nodeUrl);
        urlMapClient.put(nodeUrl, client);
        return client;
    }

    public ScmConfClient getClientByService(String serviceName) {
        if (serviceMapClient.containsKey(serviceName)) {
            return serviceMapClient.get(serviceName);
        }

        ScmConfClient client = scmFeignClient.builder().exceptionConverter(exceptionConvertor)
                .serviceTarget(ScmConfClient.class, serviceName.toLowerCase());
        serviceMapClient.put(serviceName, client);
        return client;
    }

}
