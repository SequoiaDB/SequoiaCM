package com.sequoiacm.infrastructure.trace;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.instrument.web.TraceFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ScmOtherNodeTracerFilter extends TraceFilter {

    public ScmOtherNodeTracerFilter(BeanFactory beanFactory) {
        super(beanFactory);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        String sampledHeader = ((HttpServletRequest) servletRequest).getHeader(Span.SAMPLED_NAME);
        if (sampledHeader == null || Span.SPAN_NOT_SAMPLED.equals(sampledHeader)) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
        else {
            super.doFilter(servletRequest, servletResponse, filterChain);
        }
    }
}
