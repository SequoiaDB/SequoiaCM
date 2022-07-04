package com.sequoiacm.cloud.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sequoiacm.infrastructure.security.auth.EnableScmAuthentication;
import com.sequoiacm.infrastructure.security.auth.ScmAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;

@EnableScmAuthentication
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ScmAuthenticationFilter scmAuthenticationFilter;

    @Autowired
    private HttpFirewall httpFirewall;

    @Bean
    AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth) {
        return auth.getOrBuild();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.httpFirewall(httpFirewall);
        super.configure(web);
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.httpBasic().disable().csrf().disable().formLogin().disable().logout().disable()
                .headers().cacheControl().disable().and().sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        httpSecurity.authorizeRequests()
                .antMatchers("/", "/**/*.html", "/**/favicon.ico", "/**/*.css", "/**/*.js",
                        "/fonts/**", "/images/**", "/img/**")
                .permitAll()
                .antMatchers("/**/internal/v1/config/site", "/**/internal/v1/config/node")
                .hasRole("AUTH_ADMIN")
                .antMatchers("/**/api/**/reload-bizconf/**", "/**/api/**/conf-properties/**",
                        "/**/internal/v1/monitor_collector/**", "/**/internal/v1/config-props",
                        "/**/internal/v1/health", "/**/internal/v1/env", "/**/internal/v1/metrics",
                        "/**/internal/v1/notify/**")
                .permitAll().antMatchers("/**/internal/**").denyAll().antMatchers("/**/api/**")
                .authenticated().anyRequest().permitAll();

        scmAuthenticationFilter.enableCache(60 * 1000);
        httpSecurity.addFilterBefore(scmAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
    }
}
