package com.sequoiacm.infrastructure.trace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.TraceFilter;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

@Order(TraceFilter.ORDER + 1)
public class ScmTracePostFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new ScmTraceRequestWrapper(request, getCurrentSpan()),
                new ScmTraceResponseWrapper(response, getCurrentSpan()));

    }

    private Span getCurrentSpan() {
        return tracer != null ? tracer.getCurrentSpan() : null;
    }

    static class ScmTraceRequestWrapper extends HttpServletRequestWrapper {

        private ScmTraceInputStreamWrapper inputStreamWrapper;
        private Span currentSpan;

        public ScmTraceRequestWrapper(HttpServletRequest request, Span currentSpan) {
            super(request);
            this.currentSpan = currentSpan;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (inputStreamWrapper == null) {
                inputStreamWrapper = new ScmTraceInputStreamWrapper(super.getInputStream(),
                        currentSpan);
            }
            return inputStreamWrapper;
        }
    }

    static class ScmTraceResponseWrapper extends HttpServletResponseWrapper {

        private ScmTraceServletOutputStreamWrapper outputStreamWrapper;

        private ScmTracePrintWriterWrapper printWriterWrapper;
        private Span currentSpan;

        public ScmTraceResponseWrapper(HttpServletResponse response, Span currentSpan) {
            super(response);
            this.currentSpan = currentSpan;
        }

        @Override
        public void flushBuffer() throws IOException {
            super.flushBuffer();
            if (currentSpan != null && currentSpan.isExportable()) {
                currentSpan.logEvent(ScmSpanLogEvenDefine.SERVER_SEND_ALL);
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStreamWrapper == null) {
                outputStreamWrapper = new ScmTraceServletOutputStreamWrapper(
                        super.getOutputStream(), currentSpan);
            }
            return outputStreamWrapper;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (printWriterWrapper == null) {
                printWriterWrapper = new ScmTracePrintWriterWrapper(super.getWriter(), currentSpan);
            }
            return printWriterWrapper;
        }
    }
}
