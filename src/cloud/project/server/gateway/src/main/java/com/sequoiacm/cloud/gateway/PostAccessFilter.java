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
import com.sequoiacm.infrastructure.common.SecurityRestField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;

public class PostAccessFilter extends ZuulFilter {
    private static final Logger logger = LoggerFactory.getLogger(PostAccessFilter.class);

    private static final String ERROR_RESPONSE_HOST_INFO = "SCM-ERROR-HOST";

    private AntPathMatcher matcher = new AntPathMatcher();
    @Value("${zuul.routes.logout.path:/logout}")
    private String logoutPath = "/logout";

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();

        if (ctx.getResponseStatusCode() != 200) {
            HttpServletRequest request = ctx.getRequest();
            String sessionId = request.getHeader(SecurityRestField.SESSION_ATTRIBUTE);

            String target = "unknown host";
            logErrorMsg("proxy=" + ctx.get("proxy"), ctx);
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

            String msg = String.format(
                    "send %s request %s from %s:%d to %s with session %s failed(status=%d)",
                    request.getMethod(), request.getRequestURI(), request.getRemoteHost(),
                    request.getRemotePort(), target, sessionId, ctx.getResponseStatusCode());
            logErrorMsg(msg, ctx);
        }

        return null;
    }

    private void logErrorMsg(String msg, RequestContext ctx) {
        if (ctx.getResponseStatusCode() == HttpStatus.NOT_FOUND.value()
                && isLogoutRequest(ctx.getRequest())) {
            // 登出过期 session 时，降低日志级别
            logger.debug(msg);
        }
        else {
            logger.error(msg);
        }
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return matcher.match(logoutPath, uri);
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
