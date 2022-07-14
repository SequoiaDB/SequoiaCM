package com.sequoiacm.infrastructure.slowlog;

import com.sequoiacm.infrastructure.slowlog.module.OperationStatistics;

import java.util.*;

public interface SlowLogContext {

    void beginOperation(String operation, boolean ignoreNestedCall);

    void beginOperation(String operation);

    void endOperation();

    String getSessionId();

    void setSessionId(String sessionId);

    String getPath();

    void setPath(String path);

    String getMethod();

    void setMethod(String method);

    Date getStart();

    void setStart(Date start);

    Date getEnd();

    void setEnd(Date end);

    long getSpend();

    long getReadClientSpend();

    void setReadClientSpend(long readClientSpend);

    void addReadClientSpend(long readClientSpend);

    long getWriteResponseSpend();

    void setWriteResponseSpend(long writeResponseSpend);

    void addWriteResponseSpend(long writeResponseSpend);

    Collection<OperationStatistics> getOperationStatisticsData();

    Map<String, Set<Object>> getExtras();

    void addExtra(String key, Object value);
}
