package com.sequoiacm.cloud.authentication.security;

import com.sequoiacm.cloud.authentication.config.LdapConfig;
import com.sequoiacm.cloud.authentication.config.TokenConfig;

public class AuthenticationOptions {
    private LdapConfig ldapConfig;
    private TokenConfig tokenConfig;

    public AuthenticationOptions(LdapConfig ldapConfig, TokenConfig tokenConfig) {
        this.ldapConfig = ldapConfig;
        this.tokenConfig = tokenConfig;
    }

    public String getUsernameAttribute() {
        return ldapConfig.getUsernameAttribute();
    }

    public boolean isTokenEnabled() {
        return tokenConfig.isEnabled();
    }

    public boolean isTokenAllowAnyValue() {
        return tokenConfig.isAllowAnyValue();
    }

    public String getTokenValue() {
        return tokenConfig.getTokenValue();
    }
}
