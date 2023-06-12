package com.sequoiacm.infrastructure.config.core.customizer;

import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConfigCustomizerMgr {
    private static final Logger logger = LoggerFactory.getLogger(ConfigCustomizerMgr.class);
    private Map<String, ConfigCustomizer> confCustomizerMap = new HashMap<>();

    public ConfigCustomizerMgr(List<ConfigCustomizer> configCustomizers) {
        for (ConfigCustomizer configCustomizer : configCustomizers) {
            BusinessType businessType = configCustomizer.getClass()
                    .getAnnotation(BusinessType.class);
            if (businessType == null) {
                throw new IllegalArgumentException(
                        "ConfigCustomizer must be annotated with BusinessType: "
                                + configCustomizer.getClass());
            }
            ConfigCustomizer old = confCustomizerMap.put(businessType.value(), configCustomizer);
            if (old != null) {
                throw new IllegalArgumentException("ConfigCustomizer duplicated: "
                        + configCustomizer.getClass() + ", " + old.getClass());
            }
        }

        logger.info("ConfigCustomizerMgr init success, configCustomizers: {}", configCustomizers);
    }

    public ConfigCustomizer get(String businessType) {
        ConfigCustomizer ret = confCustomizerMap.get(businessType);
        if (ret == null) {
            return DefaultConfigCustomizer.DEFAULT;
        }
        return ret;
    }
}
