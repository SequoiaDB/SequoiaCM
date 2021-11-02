package com.sequoiacm.infrastructure.feign;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// AutoConfig by resources/META-INF/spring.factories
@ConfigurationProperties(prefix = "scm.feign")
@Component
public class ScmFeignConfig {

    private int connectTimeout = 10 * 1000;
    private int readTimeout = 60 * 1000;

    public int getConnectTimeout() {
        return connectTimeout;
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
