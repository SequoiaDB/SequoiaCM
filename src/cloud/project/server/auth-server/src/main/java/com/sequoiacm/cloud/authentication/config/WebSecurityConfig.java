package com.sequoiacm.cloud.authentication.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.savedrequest.NullRequestCache;

import com.sequoiacm.cloud.authentication.dao.SequoiadbScmUserRoleRepository;
import com.sequoiacm.cloud.authentication.security.AuthenticationOptions;
import com.sequoiacm.cloud.authentication.security.Http401UnauthorizedEntryPoint;
import com.sequoiacm.cloud.authentication.security.ScmAuthenticationConfigurer;
import com.sequoiacm.cloud.authentication.security.ScmAuthenticationFailureHandler;
import com.sequoiacm.cloud.authentication.security.ScmAuthenticationSuccessHandler;
import com.sequoiacm.cloud.authentication.security.ScmLogoutSuccessHandler;
import com.sequoiacm.cloud.authentication.security.ScmUserDetailsService;
import com.sequoiacm.cloud.authentication.security.SignatureInfoDetailSource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiadb.base.SequoiadbDatasource;

@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SequoiadbDatasource sequoiadbDatasource;

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private LdapConfig ldapConfig;

    @Autowired
    private TokenConfig tokenConfig;

    @Autowired
    private CollectionConfig collectionConfig;

    @Autowired
    private ScmAudit audit;

    @Bean
    ScmUserRoleRepository userRoleRepository() {

        return new SequoiadbScmUserRoleRepository(sequoiadbDatasource,
                collectionConfig.getCollectionSpaceName(), collectionConfig.getUserCollectionName(),
                collectionConfig.getRoleCollectionName(), passwordEncoder());
    }

    @Bean
    ScmUserDetailsService scmUserDetailsService() {
        return new ScmUserDetailsService(userRoleRepository());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    ScmAuthenticationConfigurer scmAuthenticationConfigurer() {
        return new ScmAuthenticationConfigurer<AuthenticationManagerBuilder>(
                scmUserDetailsService(), ldapTemplate,
                new AuthenticationOptions(ldapConfig, tokenConfig))
                        .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.apply(scmAuthenticationConfigurer());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().httpBasic().disable().formLogin()
                .authenticationDetailsSource(new SignatureInfoDetailSource())
                .successHandler(new ScmAuthenticationSuccessHandler(audit))
                .failureHandler(new ScmAuthenticationFailureHandler()).permitAll().and().logout()
                .logoutSuccessHandler(new ScmLogoutSuccessHandler(audit)).and().exceptionHandling()
                .authenticationEntryPoint(new Http401UnauthorizedEntryPoint()).and().requestCache()
                .requestCache(new NullRequestCache()).and().authorizeRequests()

                .antMatchers(HttpMethod.GET, "/api/**/resources/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/roles/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/privileges/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/relations/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/users/**").permitAll()

                .antMatchers(HttpMethod.POST, "/api/**").hasRole(ScmRole.AUTH_ADMIN_SHORT_NAME)
                .antMatchers(HttpMethod.DELETE, "/api/**").hasRole(ScmRole.AUTH_ADMIN_SHORT_NAME)
                .antMatchers(HttpMethod.PUT, "/api/**").hasRole(ScmRole.AUTH_ADMIN_SHORT_NAME)
                .antMatchers(HttpMethod.GET, "/api/**").authenticated().anyRequest().permitAll();
    }
}
