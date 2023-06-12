package com.sequoiacm.cloud.gateway;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.infrastructure.security.common.AuthCommonTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.sequoiacm.infrastructure.common.SecurityRestField;

public class AccessFilter extends ZuulFilter {
    private static final Logger logger = LoggerFactory.getLogger(AccessFilter.class);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        String sessionId = request.getHeader(SecurityRestField.SESSION_ATTRIBUTE);
        if (sessionId != null) {
            logger.debug("send {} request to {} with session {}", request.getMethod(),
                    request.getRequestURI(), sessionId);
            String user = (String) request.getAttribute(SecurityRestField.USER_ATTRIBUTE);
            if (user != null && !AuthCommonTools.isBigUser(user, request.getCharacterEncoding())) {
                ctx.addZuulRequestHeader(SecurityRestField.USER_ATTRIBUTE, user);
            }
        }
        else {
            logger.debug("send {} request to {} without session", request.getMethod(),
                    request.getRequestURI());
        }
        return null;
    }
}
