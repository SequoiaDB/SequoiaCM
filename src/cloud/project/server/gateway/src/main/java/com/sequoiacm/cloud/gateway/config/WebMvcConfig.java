package com.sequoiacm.cloud.gateway.config;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.cloud.gateway.*;
import com.sequoiacm.cloud.gateway.filter.ScmRequestPreHandleFilter;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;


@Configuration
public class WebMvcConfig {

    @Bean
    public AccessFilter accessFilter() {
        return new AccessFilter();
    }

    @Bean
    public HttpFirewall customHttpFirewall() {
        GatewayHttpFireWall f = new GatewayHttpFireWall();
        f.setAllowUrlEncodedSlash(true);
        return f;
    }

    @Bean
    public PostAccessFilter postAccessFilter() {
        return new PostAccessFilter();
    }

    @Bean
    ScmSendResponseFilter scmSendResponseFilter(ZuulProperties zuulProperties) {
        return new ScmSendResponseFilter(zuulProperties);
    }

    @Bean
    ScmSendErrorFilter scmSendErrorFilter(){
        return new ScmSendErrorFilter();
    }


    @Bean
    public DefaultErrorAttributes errorAttributes(DiscoveryClient discoveryClient) {
        return new ErrorAttributes(discoveryClient);
    }

    @Bean
    public FilterRegistrationBean requestPreHandleFilter(ScmRequestPreHandleFilter requestPreHandleFilter) {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(
                requestPreHandleFilter);
        filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE,
                REQUEST);
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver() {
            @Override
            public boolean isMultipart(HttpServletRequest request) {
                // Same check as in Commons FileUpload...
                String method = request.getMethod();
                if (!"post".equalsIgnoreCase(method) && !"put".equalsIgnoreCase(method)) {
                    return false;
                }
                String contentType = request.getContentType();
                return StringUtils.startsWithIgnoreCase(contentType, "multipart/");
            }
        };
    }
}
