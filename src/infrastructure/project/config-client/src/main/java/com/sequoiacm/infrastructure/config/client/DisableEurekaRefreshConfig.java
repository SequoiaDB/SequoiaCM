package com.sequoiacm.infrastructure.config.client;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.CloudEurekaInstanceConfig;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

// see org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration
// org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration

@Configuration
@ConditionalOnClass(RefreshScope.class)
@ConditionalOnProperty(prefix = "scm.eureka", name = "enableRefresh", havingValue = "false", matchIfMissing = true)
public class DisableEurekaRefreshConfig {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private AbstractDiscoveryClientOptionalArgs<?> optionalArgs;

    @Bean(destroyMethod = "shutdown")
    public EurekaClient eurekaClient(ApplicationInfoManager manager, EurekaClientConfig config) {
        return new CloudEurekaClient(manager, config, this.optionalArgs, this.context);
    }

    @Bean
    public ApplicationInfoManager eurekaApplicationInfoManager(EurekaInstanceConfig config) {
        InstanceInfo instanceInfo = new InstanceInfoFactory().create(config);
        return new ApplicationInfoManager(config, instanceInfo);
    }

    @Bean
    @ConditionalOnBean(AutoServiceRegistrationProperties.class)
    @ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
    public EurekaRegistration eurekaRegistration(EurekaClient eurekaClient,
            CloudEurekaInstanceConfig instanceConfig, ApplicationInfoManager applicationInfoManager,
            @Autowired(required = false) HealthCheckHandler healthCheckHandler) {
        return EurekaRegistration.builder(instanceConfig).with(applicationInfoManager)
                .with(eurekaClient).with(healthCheckHandler).build();
    }

    @Bean
    public EurekaClientRefreshIntercepter eurekaClientRefreshIntercepter() {
        return new EurekaClientRefreshIntercepter();
    }

}

@Aspect
class EurekaClientRefreshIntercepter {

    @Around("execution(* org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration.EurekaClientConfigurationRefresher.onApplicationEvent(..))")
    public Object aroundMethod(ProceedingJoinPoint pjd) {
        return null;
    }
}
