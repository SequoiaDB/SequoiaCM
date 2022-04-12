package com.sequoiacm.cloud.gateway.filter;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class ZuulPrefixForwardDecider implements CustomForwardDecider {
    private static final String ZUUL_PREFIX = "/zuul/";

    @Override
    public Decision decide(HttpServletRequest req) {
        // url == /zuul/serviceName/targetApi 是我们需要拦截的
        String url = req.getRequestURI();
        if (!url.startsWith(ZUUL_PREFIX)) {
            return Decision.shouldNotForward();
        }
        String urlNoZuulPrefix = url.substring(ZUUL_PREFIX.length()).trim();
        if (urlNoZuulPrefix.length() <= 0) {
            return Decision.shouldNotForward();
        }

        // urlNoZuulPrefix = serviceName/XXX
        int firstDelimiterIndex = urlNoZuulPrefix.indexOf("/");
        if (firstDelimiterIndex == -1) {
            // urlNoZuulPrefix = serviceName
            return Decision.shouldForward(urlNoZuulPrefix, "/", true);
        }

        String serviceName = urlNoZuulPrefix.substring(0, firstDelimiterIndex);
        String targetApi = urlNoZuulPrefix.substring(firstDelimiterIndex);
        return Decision.shouldForward(serviceName, targetApi, true);
    }

    @Override
    public String toString() {
        return "ZuulPrefixForwardDecider";
    }
}
