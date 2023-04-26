package com.sequoiacm.infrastructure.config.client.remote;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.netflix.feign.support.SpringEncoder;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;


@Component
public class ScmConfFeignClientFactory {
    @Autowired
    private ScmFeignClient feignClient;
    private ScmConfFeignClient instance;
    @Autowired
    private ObjectFactory<HttpMessageConverters> convertor;
    private ScmConfServerExceptionConvertor exceptionConvertor = new ScmConfServerExceptionConvertor();

    public ScmConfFeignClient getClient() {
        if (instance != null) {
            return instance;
        }
        ScmConfFeignClient client = feignClient.builder()
                .encoder(new SpringEncoder(convertor))
                .exceptionConverter(exceptionConvertor)
                .serviceTarget(ScmConfFeignClient.class, "config-server");
        this.instance = client;
        return client;
    }
}
