package com.sequoiacm.infrastructure.trace;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.TraceFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class ScmGatewayTraceFilter extends TraceFilter {

    private ScmTraceConfig traceConfig;
    private Tracer tracer;


    public ScmGatewayTraceFilter(BeanFactory beanFactory, ScmTraceConfig traceConfig, Tracer tracer) {
        super(beanFactory);
        this.traceConfig = traceConfig;
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        if (traceConfig.isEnabled() && traceConfig.getSamplePercentage() > 0) {
            super.doFilter(servletRequest, servletResponse, filterChain);
        }
        else {
            tracer.continueSpan(Span.builder().exportable(false).build());
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
