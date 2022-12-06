package com.sequoiacm.client.element.trace;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

/**
 * The trace info.
 */
public class ScmTrace {

    private String traceId;
    private long duration;
    private String requestUrl;
    private int spanCount;
    private boolean isComplete;
    private ScmTraceSpan firstSpan;

    public ScmTrace(BasicBSONList spanBsonList) throws ScmException {
        if (spanBsonList == null || spanBsonList.isEmpty()) {
            throw new ScmInvalidArgumentException("spanBsonList is empty");
        }
        List<ScmTraceSpan> spanList = new ArrayList<ScmTraceSpan>();
        for (Object o : spanBsonList) {
            BSONObject spanBson = (BSONObject) o;
            spanList.add(new ScmTraceSpan(spanBson));
        }
        for (ScmTraceSpan scmTraceSpan : spanList) {
            scmTraceSpan.setNextSpans(getSpanChildren(scmTraceSpan.getSpanId(), spanList));
            if (scmTraceSpan.getParentId() == null) {
                isComplete = true;
            }
        }
        this.firstSpan = spanList.get(0);
        this.traceId = firstSpan.getTraceId();
        this.requestUrl = firstSpan.getRequestUrl();
        this.duration = firstSpan.getDuration();
        this.spanCount = spanList.size();
    }

    private List<ScmTraceSpan> getSpanChildren(String spanId, List<ScmTraceSpan> spanList) {
        List<ScmTraceSpan> children = new ArrayList<ScmTraceSpan>();
        for (ScmTraceSpan scmTraceSpan : spanList) {
            if (spanId.equals(scmTraceSpan.getParentId())) {
                children.add(scmTraceSpan);
            }
        }
        return children;
    }

    /**
     * Return the trace id.
     * 
     * @return The trace id.
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Return the duration time of the trace with microseconds.
     * 
     * @return The duration time of the trace with microseconds.
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Return the request url of the trace.
     * 
     * @return The request url.
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * Return the span count of the trace.
     * 
     * @return The span count.
     */
    public int getSpanCount() {
        return spanCount;
    }

    /**
     * Return the first span of the trace.
     * 
     * @return The first span
     */
    public ScmTraceSpan getFirstSpan() {
        return firstSpan;
    }

    /**
     * Return whether the trace is complete.
     * 
     * @return Whether the trace is complete.
     */
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public String toString() {
        return "ScmTrace{" + "traceId='" + traceId + '\'' + ", duration=" + duration
                + ", requestUrl='" + requestUrl + '\'' + ", spanCount=" + spanCount
                + ", isComplete=" + isComplete + ", firstSpan=" + firstSpan + '}';
    }
}
