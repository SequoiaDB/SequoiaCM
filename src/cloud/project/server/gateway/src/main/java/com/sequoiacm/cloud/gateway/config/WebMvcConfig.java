package com.sequoiacm.cloud.gateway.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import com.sequoiacm.cloud.gateway.AccessFilter;
import com.sequoiacm.cloud.gateway.ErrorAttributes;
import com.sequoiacm.cloud.gateway.PostAccessFilter;
import com.sequoiacm.cloud.gateway.ScmSendResponseFilter;
import com.sequoiacm.cloud.gateway.filter.StatisticReqPostFilter;
import com.sequoiacm.cloud.gateway.filter.StatisticReqPreFilter;

@Configuration
public class WebMvcConfig {

    @Bean
    public AccessFilter accessFilter() {
        return new AccessFilter();
    }

    @Bean
    public HttpFirewall customHttpFirewal() {
        StrictHttpFirewall f = new StrictHttpFirewall();
        f.setAllowUrlEncodedSlash(true);
        return f;
    }

    @Bean
    public StatisticReqPreFilter statisticReqPreFilter() {
        return new StatisticReqPreFilter();
    }

    @Bean
    public StatisticReqPostFilter statisticReqPostFilter() {
        return new StatisticReqPostFilter();
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
