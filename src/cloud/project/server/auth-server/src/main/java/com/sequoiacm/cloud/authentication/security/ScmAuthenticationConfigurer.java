package com.sequoiacm.cloud.authentication.security;

import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.ProviderManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.UserDetailsAwareConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ScmAuthenticationConfigurer<B extends ProviderManagerBuilder<B>>
        extends UserDetailsAwareConfigurer<B, UserDetailsService> {
    private ScmAuthenticationProvider provider;

    private UserDetailsService userDetailsService;

    public ScmAuthenticationConfigurer(ScmUserDetailsService userDetailsService,
            LdapTemplate ldapTemplate, AuthenticationOptions authenticationOptions) {
        this.userDetailsService = userDetailsService;
        provider = new ScmAuthenticationProvider(ldapTemplate, authenticationOptions,
                userDetailsService);
    }

    @SuppressWarnings("unchecked")
    public ScmAuthenticationConfigurer withObjectPostProcessor(
            ObjectPostProcessor<?> objectPostProcessor) {
        addObjectPostProcessor(objectPostProcessor);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ScmAuthenticationConfigurer passwordEncoder(PasswordEncoder passwordEncoder) {
        provider.setPasswordEncoder(passwordEncoder);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ScmAuthenticationConfigurer passwordEncoder(
            org.springframework.security.authentication.encoding.PasswordEncoder passwordEncoder) {
        provider.setPasswordEncoder(passwordEncoder);
        return this;
    }

    @Override
    public void configure(B builder) throws Exception {
        provider = postProcess(provider);
        builder.authenticationProvider(provider);
    }

    @Override
    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

}
