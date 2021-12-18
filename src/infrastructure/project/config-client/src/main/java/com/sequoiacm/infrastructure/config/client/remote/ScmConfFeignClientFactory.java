package com.sequoiacm.infrastructure.config.client.remote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;

import feign.Request.Options;

@Component
public class ScmConfFeignClientFactory {
    @Autowired
    private ScmFeignClient feignClient;
    private ScmConfFeignClient instance;
    private ScmConfServerExceptionConvertor exceptionConvertor = new ScmConfServerExceptionConvertor();

    public ScmConfFeignClient getClient() {
        if (instance != null) {
            return instance;
        }
        ScmConfFeignClient client = feignClient.builder()
                .exceptionConverter(exceptionConvertor)
                .serviceTarget(ScmConfFeignClient.class, "config-server");
        this.instance = client;
        return client;
    }
}
