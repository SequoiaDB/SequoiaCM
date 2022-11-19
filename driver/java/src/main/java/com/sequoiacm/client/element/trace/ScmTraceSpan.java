package com.sequoiacm.client.element.trace;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The trace span info.
 */
public class ScmTraceSpan {

    private static final String REQUEST_URL_TAG_KEY = "http.url";
    private static final String SERVICE_RECEIVED_KEY = "sr";

    private String traceId;
    private String spanId;
    private String parentId;
    private String name;
    private long duration;
    private String requestUrl;
    private String service;
    private String ip;
    private int port;
    private long timestamp;
    private Map<String, List<String>> tags;
    private List<ScmTraceSpan> nextSpans;
    private List<ScmTraceAnnotation> annotations;

    public ScmTraceSpan(BSONObject spanBson) throws ScmException {
        this.traceId = BsonUtils.getStringChecked(spanBson, FieldName.TraceSpan.TRACE_ID);
        this.spanId = BsonUtils.getStringChecked(spanBson, FieldName.TraceSpan.SPAN_ID);
        this.parentId = BsonUtils.getString(spanBson, FieldName.TraceSpan.PARENT_SPAN_ID);
        this.duration = BsonUtils.getNumberChecked(spanBson, FieldName.TraceSpan.DURATION)
                .longValue();
        this.name = BsonUtils.getStringChecked(spanBson, FieldName.TraceSpan.SPAN_NAME);
        this.timestamp = BsonUtils.getLongChecked(spanBson, FieldName.TraceSpan.TIMESTAMP) / 1000;
        BasicBSONList tagBsonList = (BasicBSONList) BsonUtils.getBSONObject(spanBson,
                FieldName.TraceSpan.TAGS);
        if (tagBsonList != null && tagBsonList.size() > 0) {
            Map<String, List<String>> tags = new HashMap<String, List<String>>();
            for (Object o : tagBsonList) {
                BSONObject tagBson = (BSONObject) o;
                String key = (String) tagBson.get("key");
                String value = (String) tagBson.get("value");
                List<String> tagValues = tags.get(key);
                if (tagValues == null) {
                    tagValues = new ArrayList<String>();
                    tagValues.add(value);
                    tags.put(key, tagValues);
                }
                else {
                    tagValues.add(value);
                }
                if (REQUEST_URL_TAG_KEY.equals(key)) {
                    this.requestUrl = value;
                }
            }
            this.tags = tags;
        }
        BasicBSONList annoBsonList = (BasicBSONList) BsonUtils.getBSONObject(spanBson,
                FieldName.TraceSpan.ANNOTATIONS);
        if (annoBsonList != null && annoBsonList.size() > 0) {
            List<ScmTraceAnnotation> annotations = new ArrayList<ScmTraceAnnotation>();
            for (Object o : annoBsonList) {
                BSONObject annotationBson = (BSONObject) o;
                ScmTraceAnnotation traceAnnotation = new ScmTraceAnnotation(annotationBson);
                if (SERVICE_RECEIVED_KEY.equals(traceAnnotation.getType())) {
                    this.service = traceAnnotation.getService();
                    this.ip = traceAnnotation.getIp();
                    this.port = traceAnnotation.getPort();
                }
                annotations.add(traceAnnotation);
            }
            this.annotations = annotations;
        }
    }

    /**
     * Return the trace id of the span.
     * 
     * @return The trace id.
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Return the span id of the span.
     *
     * @return The span id.
     */
    public String getSpanId() {
        return spanId;
    }

    /**
     * Return the parent span id of the span.
     *
     * @return The parent span id.
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Return the duration time of the span with microseconds.
     *
     * @return The duration time of the span with microseconds.
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Return the request url of the span.
     *
     * @return The request url.
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * Return the service name of the span.
     *
     * @return The service name.
     */
    public String getService() {
        return service;
    }

    /**
     * Return the ip of the span.
     *
     * @return The ip address.
     */
    public String getIp() {
        return ip;
    }

    /**
     * Return the port of the span.
     *
     * @return The port of the span.
     */
    public int getPort() {
        return port;
    }

    /**
     * Return the timestamp of the span.
     *
     * @return The timestamp of the span.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Return the tags of the span.
     *
     * @return The tags of the span.
     */
    public Map<String, List<String>> getTags() {
        return tags;
    }

    /**
     * Return the next spans of the span.
     *
     * @return The next spans of the span.
     */
    public List<ScmTraceSpan> getNextSpans() {
        return nextSpans;
    }

    /**
     * Return the annotations of the span.
     *
     * @return The annotations of the span.
     */
    public List<ScmTraceAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * Return the name of the span.
     *
     * @return The name of the span.
     */
    public String getName() {
        return name;
    }

    void setNextSpans(List<ScmTraceSpan> nextSpans) {
        this.nextSpans = nextSpans;
    }

    @Override
    public String toString() {
        return "ScmTraceSpan{" + "traceId='" + traceId + '\'' + ", spanId='" + spanId + '\''
                + ", parentId='" + parentId + '\'' + ", duration=" + duration + ", requestUrl='"
                + requestUrl + '\'' + ", service='" + service + '\'' + ", ip='" + ip + '\''
                + ", port=" + port + ", timestamp=" + timestamp + ", tags=" + tags + ", nextSpans="
                + nextSpans + ", annotations=" + annotations + '}';
    }
}
