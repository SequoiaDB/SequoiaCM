package com.sequoiacm.om.omserver.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public TomcatEmbeddedServletContainerFactory containerCustomizer() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.addConnectorCustomizers(new RelaxedQueryCharsConnectorCustomizer());
        return factory;
    }

    /**
     * Parameters are allowed to have '[]' special characters.
     */
    static class RelaxedQueryCharsConnectorCustomizer implements TomcatConnectorCustomizer {
        @Override
        public void customize(Connector connector) {
            connector.setAttribute("relaxedQueryChars", "[]");
        }
    }
}
