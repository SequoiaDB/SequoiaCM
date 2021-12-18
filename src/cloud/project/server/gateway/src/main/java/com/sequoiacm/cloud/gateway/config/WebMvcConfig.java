package com.sequoiacm.cloud.gateway.config;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.cloud.gateway.*;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;


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
    public DefaultErrorAttributes errorAttributes() {
        return new ErrorAttributes();
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
