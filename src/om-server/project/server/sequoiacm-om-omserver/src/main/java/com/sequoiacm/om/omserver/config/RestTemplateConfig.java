package com.sequoiacm.om.omserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(ScmOmServerConfig scmOmServerConfig) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(scmOmServerConfig.getConnectTimeout());
        factory.setReadTimeout(scmOmServerConfig.getReadTimeout());
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
