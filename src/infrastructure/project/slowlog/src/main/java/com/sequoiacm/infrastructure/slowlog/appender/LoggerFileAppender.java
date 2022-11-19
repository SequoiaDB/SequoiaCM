package com.sequoiacm.infrastructure.slowlog.appender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerFileAppender implements SlowLogAppender {

    private static final Logger slowLogger = LoggerFactory.getLogger("slowlog");

    @Override
    public void log(String log) {
        if (log == null) {
            return;
        }
        slowLogger.warn(log);
    }

    @Override
    public String getName() {
        return SlowLogAppender.LOGGER;
    }
}
