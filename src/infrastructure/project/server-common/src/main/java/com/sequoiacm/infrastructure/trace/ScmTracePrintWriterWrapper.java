package com.sequoiacm.infrastructure.trace;

import org.springframework.cloud.sleuth.Span;

import java.io.PrintWriter;
import java.io.Writer;

public class ScmTracePrintWriterWrapper extends PrintWriter {

    private final Span span;

    public ScmTracePrintWriterWrapper(Writer out, Span span) {
        super(out);
        this.span = span;
    }

    @Override
    public void flush() {
        super.flush();
        recordSSALog();
    }

    @Override
    public void close() {
        super.close();
        recordSSALog();
    }

    private void recordSSALog() {
        if (span != null && span.isExportable()) {
            span.logEvent(ScmSpanLogEvenDefine.SERVER_SEND_ALL);
        }
    }
}
