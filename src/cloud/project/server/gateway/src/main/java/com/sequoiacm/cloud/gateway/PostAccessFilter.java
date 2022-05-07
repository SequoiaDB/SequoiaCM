package com.sequoiacm.cloud.gateway;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.cloud.gateway.config.ApacheHttpClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.client.http.HttpResponse;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.sequoiacm.infrastructure.security.auth.RestField;

public class PostAccessFilter extends ZuulFilter {
    private static final Logger logger = LoggerFactory.getLogger(PostAccessFilter.class);

    private static final String ERROR_RESPONSE_HOST_INFO = "SCM-ERROR-HOST";

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();

        if (ctx.getResponseStatusCode() != 200) {
            HttpServletRequest request = ctx.getRequest();
            String sessionId = request.getHeader(RestField.SESSION_ATTRIBUTE);

            String target = "unknown host";
            logger.error("proxy=" + ctx.get("proxy"));
            Object o = ctx.get("ribbonResponse");
            if (o instanceof HttpResponse) {
                HttpResponse rar = (HttpResponse) o;
                target = rar.getRequestedURI().getHost() + ":" + rar.getRequestedURI().getPort();
            }
            else {
                HttpServletResponse response = ctx.getResponse();
                target = response.getHeader(ERROR_RESPONSE_HOST_INFO);
            }

            if (target == null) {
                target = String
                        .valueOf(ctx.get(ApacheHttpClientConfiguration.HTTP_CLIENT_TARGET_HOST));
            }

            logger.error("send {} request {} from {}:{} to {} with session {} failed(status={})",
                    request.getMethod(), request.getRequestURI(), request.getRemoteHost(),
                    request.getRemotePort(), target, sessionId, ctx.getResponseStatusCode());
        }

        return null;
    }

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return Integer.MAX_VALUE;
    }
}
