package com.sequoiacm.cloud.authentication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@ConfigurationProperties(prefix = "scm.auth.token")
public class TokenConfig {
    private boolean enabled = false;
    private boolean allowAnyValue = false;
    private String tokenValue = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowAnyValue() {
        return allowAnyValue;
    }

    public void setAllowAnyValue(boolean allowAnyValue) {
        this.allowAnyValue = allowAnyValue;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        Assert.hasLength(tokenValue, "token cannot be empty");
        this.tokenValue = tokenValue;
    }
}
