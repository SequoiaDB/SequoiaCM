package com.sequoiacm.infrastructure.config.client.props;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

@Component
public class ScmCommonUtil {
    @Autowired
    private ApplicationContext context;

    private String localConfigFilePropertiesSourceName = null;

    public String getLocalConfigFilePropName() {
        if (localConfigFilePropertiesSourceName != null) {
            return localConfigFilePropertiesSourceName;
        }

        String configFilePath = context.getEnvironment()
                .getProperty(ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY);
        configFilePath = StringUtils.cleanPath(configFilePath);
        if (!ResourceUtils.isUrl(configFilePath)) {
            this.localConfigFilePropertiesSourceName = "applicationConfig: ["
                    + ResourceUtils.FILE_URL_PREFIX + configFilePath + "]";
            return localConfigFilePropertiesSourceName;
        }
        throw new IllegalArgumentException("local config file is unknown: " + configFilePath);
    }

    public Map<String, Object> getScmConfBeans() {
        Map<String, Object> ret = new HashMap<>();
        Map<String, Object> configPropBean = context
                .getBeansWithAnnotation(ConfigurationProperties.class);
        for (Map.Entry<String, Object> entry : configPropBean.entrySet()) {
            if (entry.getValue().getClass().getPackage().getName().startsWith("com.sequoiacm")) {
                ret.put(entry.getKey(), entry.getValue());
            }
        }
        return ret;
    }

    public Map<String, Object> getRefreshableBeans() {
        return context.getBeansWithAnnotation(RefreshScope.class);
    }
}