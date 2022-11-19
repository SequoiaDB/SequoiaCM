package com.sequoiacm.cloud.gateway.forward.decider;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@Order(ZuulPrefixForwardDecider.ORDER)
public class ZuulPrefixForwardDecider implements ForwardDecider {

    public static final int ORDER = 1;

    private static final String ZUUL_PREFIX = "/zuul/";
    private static final String default_contentType = "binary/octet-stream";

    @Override
    public Decision decide(HttpServletRequest req) {
        // url == /zuul/serviceName/targetApi 是我们需要拦截的
        String url = req.getRequestURI();
        if (!url.startsWith(ZUUL_PREFIX)) {
            return Decision.unrecognized();
        }
        String urlNoZuulPrefix = url.substring(ZUUL_PREFIX.length()).trim();
        if (urlNoZuulPrefix.length() <= 0) {
            return Decision.unrecognized();
        }

        // urlNoZuulPrefix = serviceName/XXX
        int firstDelimiterIndex = urlNoZuulPrefix.indexOf("/");
        if (firstDelimiterIndex == -1) {
            // urlNoZuulPrefix = serviceName
            return Decision.shouldCustomForward(urlNoZuulPrefix, "/", default_contentType, true, true);
        }

        String serviceName = urlNoZuulPrefix.substring(0, firstDelimiterIndex);
        String targetApi = urlNoZuulPrefix.substring(firstDelimiterIndex);
        return Decision.shouldCustomForward(serviceName, targetApi, default_contentType, true, true);
    }

    @Override
    public String toString() {
        return "ZuulPrefixForwardDecider";
    }
}
