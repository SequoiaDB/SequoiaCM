package com.sequoiacm.infrastructure.trace;

import org.springframework.cloud.sleuth.Log;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanAdjuster;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScmSpanAdjuster implements SpanAdjuster {
    @Override
    public Span adjust(Span span) {
        // 去重重复的 ssa 事件
        List<Log> logs = new ArrayList<>(span.logs());
        Log lastSSAEvent = null;
        Iterator<Log> iterator = logs.iterator();
        int ssaCount = 0;
        while (iterator.hasNext()) {
            Log next = iterator.next();
            if (next.getEvent().equals(ScmSpanLogEvenDefine.SERVER_SEND_ALL)) {
                if (lastSSAEvent == null || lastSSAEvent.getTimestamp() < next.getTimestamp()) {
                    lastSSAEvent = next;
                }
                ssaCount++;
                iterator.remove();
            }
        }
        if (ssaCount <= 1) {
            return span;
        }
        logs.add(lastSSAEvent);
        return span.toBuilder().logs(logs).build();
    }
}
