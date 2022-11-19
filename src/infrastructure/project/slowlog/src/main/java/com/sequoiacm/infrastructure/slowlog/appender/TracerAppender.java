package com.sequoiacm.infrastructure.slowlog.appender;

import org.springframework.cloud.sleuth.Tracer;

public class TracerAppender implements SlowLogAppender {

    private static final String TAG_KEY = "slowlog";

    private Tracer tracer; // nullable

    public TracerAppender(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void log(String log) {
        if (log != null && tracer != null) {
            tracer.addTag(TAG_KEY, log);
        }
    }

    @Override
    public String getName() {
        return SlowLogAppender.TRACER;
    }
}
