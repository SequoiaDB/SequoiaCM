package com.sequoiacm.cloud.gateway.statistics.config;

import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "scm.statistics")
public class ScmStatisticsConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScmStatisticsConfig.class);
    private List<String> types;

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        if (!ScmStatisticsType.ALL_TYPES.containsAll(types)) {
            throw new IllegalArgumentException("scm.statistics.types contains unknown types:"
                    + types + ", all supported types:" + ScmStatisticsType.ALL_TYPES);
        }
        this.types = types;
    }

    @Override
    public String toString() {
        return "ScmStatisticsConfig{" + "types=" + types + '}';
    }
}
