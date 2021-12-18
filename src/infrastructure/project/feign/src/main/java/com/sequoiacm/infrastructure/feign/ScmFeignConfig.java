package com.sequoiacm.infrastructure.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// AutoConfig by resources/META-INF/spring.factories
@ConfigurationProperties(prefix = "scm.feign")
@Component
public class ScmFeignConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScmFeignConfig.class);

    private static final String PROPERTY_RIBBON_READ_TIMEOUT = "ribbon.ReadTimeout";
    private static final String PROPERTY_RIBBON_CONNECT_TIMEOUT = "ribbon.ConnectTimeout";
    private static final String PROPERTY_FEIGN_READ_TIMEOUT = "scm.feign.readTimeout";
    private static final String PROPERTY_FEIGN_CONNECT_TIMEOUT = "scm.feign.connectTimeout";

    private int connectTimeout = 10 * 1000;
    private int readTimeout = 30 * 1000;

    private Environment environment;

    public ScmFeignConfig(Environment environment) {
        this.environment = environment;
        this.connectTimeout = getPreferredValue(PROPERTY_FEIGN_CONNECT_TIMEOUT,
                PROPERTY_RIBBON_CONNECT_TIMEOUT, connectTimeout);
        this.readTimeout = getPreferredValue(PROPERTY_FEIGN_READ_TIMEOUT,
                PROPERTY_RIBBON_READ_TIMEOUT, readTimeout);
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    private int getPreferredValue(String firstPropertyKey, String secondPropertyKey,
            int defaultValue) {
        String firstPropertyValue = environment.getProperty(firstPropertyKey);
        if (firstPropertyValue != null) {
            return Integer.parseInt(firstPropertyValue);
        }
        String secondPropertyValue = environment.getProperty(secondPropertyKey);
        if (secondPropertyValue != null) {
            logger.info("ScmFeignConfig: {} is not exist, use {}, value:{}", firstPropertyKey,
                    secondPropertyKey, secondPropertyValue);
            return Integer.parseInt(secondPropertyValue);
        }
        return defaultValue;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
