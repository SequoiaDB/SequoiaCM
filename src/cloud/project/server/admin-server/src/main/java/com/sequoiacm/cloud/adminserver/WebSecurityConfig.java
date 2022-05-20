package com.sequoiacm.cloud.adminserver;

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
import com.sequoiacm.infrastructure.security.auth.ScmAuthFromHeaderFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

@EnableScmAuthentication
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ScmAuthFromHeaderFilter scmAuthenticationFilter;

    @Bean
    AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth) {
        return auth.getOrBuild();
    }

    @Bean
    public HttpFirewall httpFirewall() {
        DefaultHttpFirewall httpFirewall = new DefaultHttpFirewall();
        httpFirewall.setAllowUrlEncodedSlash(true);
        return httpFirewall;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        web.httpFirewall(httpFirewall());
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
        .httpBasic().disable()
        .csrf().disable()
        .headers().cacheControl().disable().and()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        httpSecurity
        .authorizeRequests()
        .antMatchers(
                "/",
                "/**/*.html",
                "/**/favicon.ico",
                "/**/*.css",
                "/**/*.js",
                "/fonts/**",
                "/images/**",
                "/img/**")
        .permitAll()
        .anyRequest().permitAll();

        httpSecurity.addFilterBefore(scmAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
