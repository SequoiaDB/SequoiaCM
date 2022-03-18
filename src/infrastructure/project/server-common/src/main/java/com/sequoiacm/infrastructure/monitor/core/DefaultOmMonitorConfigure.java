package com.sequoiacm.infrastructure.monitor.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnMissingBean(DefaultOmMonitorConfigure.class)
public class DefaultOmMonitorConfigure {

    public boolean allowManagementPortEqualsServerPort() {
        return true;
    }

    public Map<String, String> configureEndpointUrlParams() {
        return null;
    }
}
