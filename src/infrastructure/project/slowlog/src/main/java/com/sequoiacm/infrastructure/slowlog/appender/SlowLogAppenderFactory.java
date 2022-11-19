package com.sequoiacm.infrastructure.slowlog.appender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SlowLogAppenderFactory {

    @Autowired(required = false)
    private Tracer tracer;

    public SlowLogAppender createAppender(String type) {
        if (SlowLogAppender.LOGGER.equals(type)) {
            return new LoggerFileAppender();
        }
        else if (SlowLogAppender.TRACER.equals(type)) {
            return new TracerAppender(tracer);
        }
        return null;
    }

    public List<SlowLogAppender> createDefaultAppenderList() {
        List<SlowLogAppender> list = new ArrayList<>();
        list.add(new LoggerFileAppender());
        list.add(new TracerAppender(tracer));
        return list;
    }
}
