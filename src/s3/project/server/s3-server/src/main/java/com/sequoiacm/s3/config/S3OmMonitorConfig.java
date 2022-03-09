package com.sequoiacm.s3.config;

import com.sequoiacm.infrastructure.monitor.core.DefaultOmMonitorConfigure;
import com.sequoiacm.infrastructure.monitor.core.InstanceHealthInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@Configuration
public class S3OmMonitorConfig extends DefaultOmMonitorConfigure {

    @Override
    public S3InstanceHealthInfoProvider configureInstanceHealthInfoProvider() {
        return new S3InstanceHealthInfoProvider();
    }

    @Override
    public boolean allowManagementPortEqualsServerPort() {
        return false;
    }

    public static class S3InstanceHealthInfoProvider implements InstanceHealthInfoProvider {

        @Autowired
        private HealthEndpoint healthEndpoint;

        @Override
        @GetMapping(value = "/health", params = "action=actuator", produces = MediaType.APPLICATION_JSON_VALUE)
        public Object getHealthInfo() {
            return healthEndpoint.invoke();
        }
    }

}
