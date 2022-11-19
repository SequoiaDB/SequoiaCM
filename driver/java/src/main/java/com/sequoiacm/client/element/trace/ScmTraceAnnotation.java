package com.sequoiacm.client.element.trace;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

/**
 * The trace annotation info.
 */
public class ScmTraceAnnotation {

    private String type;

    private long timestamp;

    private String service;

    private String ip;

    private int port;

    public ScmTraceAnnotation(BSONObject annotationBson) throws ScmException {
        this.type = BsonUtils.getString(annotationBson, FieldName.TraceAnnotation.VALUE);
        this.timestamp = BsonUtils.getLong(annotationBson, FieldName.TraceAnnotation.TIMESTAMP)
                / 1000;
        BSONObject endpointBson = BsonUtils.getBSONObject(annotationBson,
                FieldName.TraceAnnotation.ENDPOINT);
        if (endpointBson != null) {
            this.service = BsonUtils.getString(endpointBson,
                    FieldName.TraceAnnotation.SERVICE_NAME);
            this.ip = BsonUtils.getString(endpointBson, FieldName.TraceAnnotation.IP);
            this.port = BsonUtils.getIntegerOrElse(endpointBson, FieldName.TraceAnnotation.PORT,
                    -1);
        }
    }

    /**
     * Return the type of the annotation.
     * 
     * @return The type.
     */
    public String getType() {
        return type;
    }

    /**
     * Return the timestamp of the annotation.
     *
     * @return The timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Return the service name of the annotation.
     *
     * @return The service name.
     */
    public String getService() {
        return service;
    }

    /**
     * Return the ip of the annotation.
     *
     * @return The ip address.
     */
    public String getIp() {
        return ip;
    }

    /**
     * Return the port of the annotation.
     *
     * @return The port.
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "ScmTraceAnnotation{" + "type='" + type + '\'' + ", timestamp=" + timestamp
                + ", service='" + service + '\'' + ", ip='" + ip + '\'' + ", port=" + port + '}';
    }
}
