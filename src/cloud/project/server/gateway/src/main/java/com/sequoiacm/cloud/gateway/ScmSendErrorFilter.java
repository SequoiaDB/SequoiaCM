package com.sequoiacm.cloud.gateway;

import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.sequoiacm.infrastructure.feign.hystrix.ScmHystrixException;
import com.sequoiacm.infrastructure.feign.hystrix.ScmHystrixUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * @see SendErrorFilter
 * 基于SendErrorFilter修改，用于转换Hystrix异常
 */
public class ScmSendErrorFilter extends ZuulFilter {

    private static final Log log = LogFactory.getLog(ScmSendErrorFilter.class);
    protected static final String SEND_ERROR_FILTER_RAN = "sendErrorFilter.ran";

    @Value("${error.path:/error}")
    private String errorPath;

    @Override
    public String filterType() {
        return ERROR_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_ERROR_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        // only forward to errorPath if it hasn't been forwarded to already
        return ctx.getThrowable() != null && !ctx.getBoolean(SEND_ERROR_FILTER_RAN, false);
    }

    @Override
    public Object run() {
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            ZuulException exception = findZuulException(ctx.getThrowable());
            HttpServletRequest request = ctx.getRequest();

            request.setAttribute("javax.servlet.error.status_code", exception.nStatusCode);


            request.setAttribute("javax.servlet.error.exception", exception);

            if (StringUtils.hasText(exception.errorCause)) {
                request.setAttribute("javax.servlet.error.message", exception.errorCause);
            }

            // custom begin
            // 转换HystrixRuntimeException
            Throwable cause = exception.getCause();
            if (cause instanceof HystrixRuntimeException) {
                String service = (String) ctx.get(SERVICE_ID_KEY);
                ScmHystrixException scmHystrixException = getScmHystrixException(service,
                        (HystrixRuntimeException) cause);
                if (scmHystrixException != null) {
                    request.setAttribute("javax.servlet.error.exception", scmHystrixException);
                    request.setAttribute("javax.servlet.error.message",
                            scmHystrixException.getMessage());
                }
            }
            // custom end

            log.warn("Error during filtering", exception);

            RequestDispatcher dispatcher = request.getRequestDispatcher(this.errorPath);
            if (dispatcher != null) {
                ctx.set(SEND_ERROR_FILTER_RAN, true);
                if (!ctx.getResponse().isCommitted()) {
                    ctx.setResponseStatusCode(exception.nStatusCode);
                    dispatcher.forward(request, ctx.getResponse());
                }
            }
        }
        catch (Exception ex) {
            ReflectionUtils.rethrowRuntimeException(ex);
        }
        return null;
    }

    private ScmHystrixException getScmHystrixException(String service, HystrixRuntimeException e) {
        HystrixRuntimeException.FailureType failureType = e.getFailureType();

        if (failureType == HystrixRuntimeException.FailureType.SHORTCIRCUIT) {
            return new ScmHystrixException(ScmHystrixUtils.formatShortCircuitedMsg(service), e);
        }
        else if (failureType == HystrixRuntimeException.FailureType.REJECTED_SEMAPHORE_EXECUTION) {
            Integer maxConcurrentRequests = HystrixCommandMetrics
                    .getInstance(HystrixCommandKey.Factory.asKey(service)).getProperties()
                    .executionIsolationSemaphoreMaxConcurrentRequests().get();
            return new ScmHystrixException(
                    ScmHystrixUtils.formatSemaphoreRejectedMsg(service, maxConcurrentRequests), e);
        }
        else if (failureType == HystrixRuntimeException.FailureType.REJECTED_THREAD_EXECUTION) {
            Integer queueSize = HystrixThreadPoolMetrics
                    .getInstance(HystrixThreadPoolKey.Factory.asKey(service)).getProperties()
                    .maxQueueSize().get();
            return new ScmHystrixException(
                    ScmHystrixUtils.formatThreadPoolRejectedMsg(service, queueSize), e);
        }
        return null;
    }

    ZuulException findZuulException(Throwable throwable) {
        if (throwable.getCause() instanceof ZuulRuntimeException) {
            // this was a failure initiated by one of the local filters
            return (ZuulException) throwable.getCause().getCause();
        }

        if (throwable.getCause() instanceof ZuulException) {
            // wrapped zuul exception
            return (ZuulException) throwable.getCause();
        }

        if (throwable instanceof ZuulException) {
            // exception thrown by zuul lifecycle
            return (ZuulException) throwable;
        }

        // fallback, should never get here
        return new ZuulException(throwable, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }
}
