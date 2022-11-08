package com.sequoiacm.cloud.authentication.config;

import com.sequoiacm.cloud.authentication.filter.ActuatorRoleFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

import com.sequoiacm.cloud.authentication.dao.SequoiadbScmUserRoleRepository;
import com.sequoiacm.cloud.authentication.security.AuthenticationOptions;
import com.sequoiacm.cloud.authentication.security.Http401UnauthorizedEntryPoint;
import com.sequoiacm.cloud.authentication.security.SCMPasswordEncoder;
import com.sequoiacm.cloud.authentication.security.ScmAuthenticationConfigurer;
import com.sequoiacm.cloud.authentication.security.ScmAuthenticationFailureHandler;
import com.sequoiacm.cloud.authentication.security.ScmAuthenticationSuccessHandler;
import com.sequoiacm.cloud.authentication.security.ScmLogoutSuccessHandler;
import com.sequoiacm.cloud.authentication.security.ScmUserDetailsService;
import com.sequoiacm.cloud.authentication.security.ScmAuthenticationDetailSource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.common.ConcurrentLruMap;
import com.sequoiacm.infrastructure.common.ConcurrentLruMapFactory;
import com.sequoiadb.base.SequoiadbDatasource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

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
    
    @Value(value = "${scm.auth.passwordEncoderCacheSize}")
    private int cacheNum;

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
        return new SCMPasswordEncoder();
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
    
    @Bean
    SaltSource saltSource() {
        return new SaltSource() {
            @Override
            public Object getSalt(UserDetails user) {
                return user;
            }
        };
    }

    @Bean
    ConcurrentLruMap<String, String> passCache() {
        return ConcurrentLruMapFactory.create(cacheNum);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().httpBasic().disable().formLogin()
                .authenticationDetailsSource(new ScmAuthenticationDetailSource())
                .successHandler(new ScmAuthenticationSuccessHandler(audit))
                .withObjectPostProcessor(
                        new ObjectPostProcessor<UsernamePasswordAuthenticationFilter>() {
                            @Override
                            public <O extends UsernamePasswordAuthenticationFilter> O postProcess(
                                    O filter) {
                                filter.setRequiresAuthenticationRequestMatcher(new OrRequestMatcher(
                                        new AntPathRequestMatcher("/login", "POST"),
                                        new AntPathRequestMatcher("/v2/localLogin", "POST")));
                                return filter;
                            }
                        })
                .failureHandler(new ScmAuthenticationFailureHandler()).permitAll().and().logout()
                .logoutSuccessHandler(new ScmLogoutSuccessHandler(audit)).and().exceptionHandling()
                .authenticationEntryPoint(new Http401UnauthorizedEntryPoint()).and().requestCache()
                .requestCache(new NullRequestCache()).and().authorizeRequests()

                .antMatchers(HttpMethod.GET, "/api/**/resources/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/roles/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/privileges/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/relations/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**/users/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/**/salt/**").permitAll()
                .antMatchers(HttpMethod.PUT,"/api/**/users/**").permitAll()

                .antMatchers(HttpMethod.POST, "/api/**").hasRole(ScmRole.AUTH_ADMIN_SHORT_NAME)
                .antMatchers(HttpMethod.DELETE, "/api/**").hasRole(ScmRole.AUTH_ADMIN_SHORT_NAME)
                .antMatchers(HttpMethod.PUT, "/api/**").hasRole(ScmRole.AUTH_ADMIN_SHORT_NAME)
                .antMatchers(HttpMethod.GET, "/api/**").authenticated().anyRequest().permitAll();

        http.addFilterAfter(new ActuatorRoleFilter(), SwitchUserFilter.class);
    }
}
