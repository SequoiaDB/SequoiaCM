package com.sequoiacm.infrastructure.monitor;

import com.sequoiacm.infrastructure.monitor.core.*;
import com.sequoiacm.infrastructure.monitor.endpoint.ScmProcessInfoEndpoint;
import com.sequoiacm.infrastructure.monitor.endpoint.ScmThreadInfoEndpoint;
import com.sequoiacm.infrastructure.monitor.endpoint.ScmTomcatConnectInfoEndpoint;
import com.sequoiacm.infrastructure.monitor.endpoint.ScmUndertowConnectInfoEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

// AutoConfig by resources/META-INF/spring.factories
@Configuration
@ConditionalOnClass({ Endpoint.class, EurekaInstanceConfigBean.class })
@ConditionalOnProperty(prefix = "eureka.client", name = "register-with-eureka", havingValue = "true")
@Import(DefaultOmMonitorConfigure.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OmMonitorAutoConfig {

    private static final Logger logger = LoggerFactory.getLogger(OmMonitorAutoConfig.class);

    private static final int DEFAULT_SERVER_PORT = 8080;

    @Bean
    public ScmProcessInfoEndpoint scmProcessInfoEndpoint() {
        return new ScmProcessInfoEndpoint();
    }

    @Bean
    public ScmThreadInfoEndpoint scmThreadInfoEndpoint() {
        return new ScmThreadInfoEndpoint();
    }

    @Bean
    @ConditionalOnClass(name = { "org.apache.catalina.Manager" })
    public ScmTomcatConnectInfoEndpoint scmTomcatConnectInfoEndpoint(Environment environment) {
        return new ScmTomcatConnectInfoEndpoint(environment.getProperty("server.port"));
    }

    @Bean
    @ConditionalOnClass(name = { "io.undertow.Undertow" })
    public ScmUndertowConnectInfoEndpoint scmUndertowConnectInfoEndpoint(
            EmbeddedWebApplicationContext applicationContext) {
        return new ScmUndertowConnectInfoEndpoint(applicationContext);
    }

    @Bean
    public ApplicationClosedListener applicationClosedListener() {
        return new ApplicationClosedListener();
    }

    @Bean
    @Conditional(ServerPortNotEqualsManagementPort.class)
    public ActuatorEndpointFilter actuatorEndpointFilter(List<Endpoint<?>> endpoints,
            DefaultOmMonitorConfigure omMonitorConfigure,
            ManagementServerProperties managementServerProperties) {
        return new ActuatorEndpointFilter(endpoints, omMonitorConfigure,
                managementServerProperties);
    }

    @Component
    public static class OmMonitorConfigureHandler {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private DefaultOmMonitorConfigure omMonitorConfigure;

        @Autowired
        private ManagementServerProperties managementServerProperties;

        @PostConstruct
        public void handleOmMonitorConfigure() throws Exception {
            handleManagementPortCheck();
        }

        private void handleManagementPortCheck() {
            if (!omMonitorConfigure.allowManagementPortEqualsServerPort()) {
                int serverPort = DEFAULT_SERVER_PORT;
                String serverPortStr = applicationContext.getEnvironment()
                        .getProperty("server.port");
                if (serverPortStr != null) {
                    serverPort = Integer.parseInt(serverPortStr);
                }
                if (serverPort == getManagementPort()) {
                    String serverName = applicationContext.getEnvironment()
                            .getProperty("spring.application.name");
                    throw new IllegalArgumentException("the management port and service port of "
                            + serverName + " must be different: managementPort="
                            + managementServerProperties.getPort() + ", servicePort=" + serverPort);
                }
            }
        }

        public int getManagementPort() {
            return managementServerProperties.getPort();
        }

    }

    private static class ServerPortNotEqualsManagementPort implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            String serverPort = environment.getProperty("server.port");
            String managementPort = environment.getProperty("management.port");
            if (serverPort == null) {
                serverPort = String.valueOf(DEFAULT_SERVER_PORT);
            }
            if (managementPort == null) {
                managementPort = serverPort;
            }
            return !Objects.equals(serverPort, managementPort);
        }
    }

}
