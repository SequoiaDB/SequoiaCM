package com.sequoiacm.infrastructure.slowlog;

import com.sequoiacm.infrastructure.slowlog.module.OperationStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SlowLogContextImpl implements SlowLogContext {

    private static final Logger sysErrorLogger = LoggerFactory.getLogger("syserror");

    private static final String SEPARATOR = ".";
    private final Map<String, OperationStatistics> operationStatisticsMap = new TreeMap<>();
    private final Stack<OperationFrame> operationFrameStack = new Stack<>();

    private Map<String, Set<Object>> extra;
    private String sessionId;
    private String path;
    private String method;
    private Date start;
    private Date end;
    private long readClientSpend;
    private long writeResponseSpend;

    private boolean allowPushStack = true;
    private int rejectedPushCount = 0;

    @Override
    public void beginOperation(String operation) {
        beginOperation(operation, false);
    }

    @Override
    public void beginOperation(String operation, boolean ignoreNestedCall) {
        if (!allowPushStack) {
            rejectedPushCount++;
            return;
        }
        OperationFrame lastFrame = null;
        try {
            lastFrame = operationFrameStack.peek();
        }
        catch (EmptyStackException ignored) {
        }
        OperationFrame operationFrame;
        long start = System.currentTimeMillis();
        if (lastFrame == null) {
            operationFrame = new OperationFrame(start, operation);
        }
        else {
            operationFrame = new OperationFrame(start, lastFrame.name + SEPARATOR + operation);
        }
        operationFrameStack.push(operationFrame);
        allowPushStack = !ignoreNestedCall;
    }

    @Override
    public void endOperation() {
        if (rejectedPushCount > 0) {
            rejectedPushCount--;
            return;
        }
        OperationFrame operationFrame = null;
        try {
            operationFrame = operationFrameStack.pop();
        }
        catch (EmptyStackException e) {
            sysErrorLogger.error("Incorrect operation, no beginOperation matching endOperation.",
                    e);
            return;
        }
        long spend = System.currentTimeMillis() - operationFrame.getStartTime();
        OperationStatistics operationStatistics = this.operationStatisticsMap
                .get(operationFrame.getName());
        if (operationStatistics == null) {
            operationStatistics = new OperationStatistics(operationFrame.getName(), spend, 1);
            this.operationStatisticsMap.put(operationFrame.getName(), operationStatistics);
        }
        else {
            operationStatistics.setSpend(operationStatistics.getSpend() + spend);
            operationStatistics.setCount(operationStatistics.getCount() + 1);
        }
        allowPushStack = true;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public Date getStart() {
        return start;
    }

    @Override
    public void setStart(Date start) {
        this.start = start;
    }

    @Override
    public Date getEnd() {
        return end;
    }

    @Override
    public void setEnd(Date end) {
        this.end = end;
    }

    @Override
    public long getSpend() {
        if (start == null || end == null) {
            return 0;
        }
        return end.getTime() - start.getTime();
    }

    @Override
    public long getReadClientSpend() {
        return readClientSpend;
    }

    @Override
    public void setReadClientSpend(long readClientSpend) {
        this.readClientSpend = readClientSpend;
    }

    @Override
    public void addReadClientSpend(long readClientSpend) {
        this.readClientSpend += readClientSpend;
    }

    @Override
    public long getWriteResponseSpend() {
        return writeResponseSpend;
    }

    @Override
    public void setWriteResponseSpend(long writeResponseSpend) {
        this.writeResponseSpend = writeResponseSpend;
    }

    @Override
    public void addWriteResponseSpend(long writeResponseSpend) {
        this.writeResponseSpend += writeResponseSpend;
    }

    @Override
    public Collection<OperationStatistics> getOperationStatisticsData() {
        return operationStatisticsMap.values();
    }

    @Override
    public Map<String, Set<Object>> getExtra() {
        return extra;
    }

    @Override
    public void addExtra(String key, Object value) {
        if (extra == null) {
            extra = new HashMap<>();
        }
        Set<Object> set = extra.get(key);
        if (set == null) {
            set = new HashSet<>(1);
            extra.put(key, set);
        }
        set.add(value);
    }

    private static class OperationFrame {

        private long startTime;
        private String name;

        public OperationFrame(long startTime, String name) {
            this.startTime = startTime;
            this.name = name;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
