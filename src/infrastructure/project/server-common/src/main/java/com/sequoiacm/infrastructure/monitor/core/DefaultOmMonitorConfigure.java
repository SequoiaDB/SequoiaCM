package com.sequoiacm.infrastructure.monitor.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnMissingBean(DefaultOmMonitorConfigure.class)
public class DefaultOmMonitorConfigure {

    @Bean
    public InstanceHealthInfoProvider configureInstanceHealthInfoProvider() {
        return new DefaultInstanceHealthInfoProvider();
    }

    public boolean allowManagementPortEqualsServerPort() {
        return true;
    }

    public static class DefaultInstanceHealthInfoProvider implements InstanceHealthInfoProvider {

        @Autowired
        private HealthEndpoint healthEndpoint;

        @Override
        public Object getHealthInfo() {
            return healthEndpoint.invoke();
        }
    }
}
