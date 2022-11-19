package com.sequoiacm.infrastructure.trace;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.TraceFilter;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(TraceFilter.ORDER - 1)
public class ScmTracePreFilter extends OncePerRequestFilter {

    private static final ThreadLocal<HttpServletRequest> requestHolder = new ThreadLocal<>();

    @Autowired(required = false)
    private Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        requestHolder.set(request);
        try {
            filterChain.doFilter(request, response);
        }
        finally {
            requestHolder.remove();
        }
    }

    public static HttpServletRequest getCurrentRequest() {
        return requestHolder.get();
    }
}
