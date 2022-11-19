package com.sequoiacm.infrastructure.slowlog.appender;

public interface SlowLogAppender {

    public final String TRACER = "TRACER";
    public final String LOGGER = "LOGGER";

    void log(String log);

    String getName();
}
