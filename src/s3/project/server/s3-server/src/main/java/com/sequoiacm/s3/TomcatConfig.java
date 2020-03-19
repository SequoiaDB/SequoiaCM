package com.sequoiacm.s3;

import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.sequoiacm.s3.authoriztion.S3AuthorizationValve;

@Component
public class TomcatConfig {

    @Bean
    public TomcatEmbeddedServletContainerFactory containerCustomizer(
            CustomContextValve contextValve, S3AuthorizationValve authVavle) {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.addContextValves(authVavle, contextValve);
        factory.addConnectorCustomizers(customConnector());
        return factory;
    }

    @Bean
    public TomcatConnectorCustomizer customConnector() {
        return new CustomConnector();
    }
}
