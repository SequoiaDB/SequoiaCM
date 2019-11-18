package com.sequoiacm.cloud.authentication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.ldap")
public class LdapConfig {
    private String usernameAttribute = "uid";

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }
}
