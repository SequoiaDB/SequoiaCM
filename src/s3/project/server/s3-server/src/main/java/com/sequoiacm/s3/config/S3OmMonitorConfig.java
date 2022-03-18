package com.sequoiacm.s3.config;

import com.sequoiacm.infrastructure.monitor.core.DefaultOmMonitorConfigure;
import org.springframework.context.annotation.Configuration;
import java.util.Collections;
import java.util.Map;

@Configuration
public class S3OmMonitorConfig extends DefaultOmMonitorConfigure {

    @Override
    public boolean allowManagementPortEqualsServerPort() {
        return false;
    }

    @Override
    public Map<String, String> configureEndpointUrlParams() {
        return Collections.singletonMap("action", "actuator");
    }

}
