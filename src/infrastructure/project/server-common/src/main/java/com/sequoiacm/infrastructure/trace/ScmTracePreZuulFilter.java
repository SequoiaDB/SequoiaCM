package com.sequoiacm.infrastructure.trace;

import com.netflix.zuul.ExecutionStatus;
import com.netflix.zuul.ZuulFilterResult;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.instrument.zuul.TracePreZuulFilter;

public class ScmTracePreZuulFilter extends TracePreZuulFilter {

    private Tracer tracer;

    public ScmTracePreZuulFilter(Tracer tracer, HttpSpanInjector spanInjector,
            HttpTraceKeysInjector httpTraceKeysInjector, ErrorParser errorParser) {
        super(tracer, spanInjector, httpTraceKeysInjector, errorParser);
        this.tracer = tracer;
    }

    @Override
    public boolean shouldFilter() {
        return tracer.getCurrentSpan() != null && tracer.getCurrentSpan().isExportable();
    }

    @Override
    public ZuulFilterResult runFilter() {
        if (tracer.getCurrentSpan() != null && tracer.getCurrentSpan().isExportable()) {
            return super.runFilter();
        }
        else {
            return new ZuulFilterResult(ExecutionStatus.SKIPPED);
        }

    }
}
