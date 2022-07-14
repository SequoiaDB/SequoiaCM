package com.sequoiacm.infrastructure.slowlog;

import com.sequoiacm.infrastructure.slowlog.module.OperationStatistics;

import java.util.*;

public class NoOpSlowLogContextImpl implements SlowLogContext {

    @Override
    public void beginOperation(String operation, boolean ignoreNestedCall) {

    }

    @Override
    public void beginOperation(String operation) {

    }

    @Override
    public void endOperation() {

    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public void setSessionId(String sessionId) {

    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void setPath(String path) {

    }

    @Override
    public String getMethod() {
        return null;
    }

    @Override
    public void setMethod(String method) {

    }

    @Override
    public Date getStart() {
        return null;
    }

    @Override
    public void setStart(Date start) {

    }

    @Override
    public Date getEnd() {
        return null;
    }

    @Override
    public void setEnd(Date end) {

    }

    @Override
    public long getSpend() {
        return 0;
    }

    @Override
    public long getReadClientSpend() {
        return 0;
    }

    @Override
    public void setReadClientSpend(long readClientSpend) {

    }

    @Override
    public void addReadClientSpend(long readClientSpend) {

    }

    @Override
    public long getWriteResponseSpend() {
        return 0;
    }

    @Override
    public void setWriteResponseSpend(long writeResponseSpend) {

    }

    @Override
    public void addWriteResponseSpend(long writeResponseSpend) {

    }

    @Override
    public Collection<OperationStatistics> getOperationStatisticsData() {
        return null;
    }

    @Override
    public Map<String, Set<Object>> getExtras() {
        return null;
    }

    @Override
    public void addExtra(String key, Object value) {

    }
}
