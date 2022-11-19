package com.sequoiacm.cloud.gateway.forward.decider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;

@Component
@Order(ZuulForwardDecider.ORDER)
public class ZuulForwardDecider implements ForwardDecider {

    public static final int ORDER = S3ForwardDecider.ORDER + 1;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    @Autowired
    private RouteLocator routeLocator;

    @Override
    public Decision decide(HttpServletRequest req) {
        final String requestURI = this.urlPathHelper.getPathWithinApplication(req);
        Route route = this.routeLocator.getMatchingRoute(requestURI);
        String serviceName = null;
        if (route != null) {
            serviceName = route.getLocation();
        }
        return Decision.shouldForward(serviceName);
    }
}
