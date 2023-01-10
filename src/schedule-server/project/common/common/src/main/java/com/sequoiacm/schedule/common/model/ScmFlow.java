package com.sequoiacm.schedule.common.model;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class ScmFlow {
    private String source;
    private String dest;

    public ScmFlow(BSONObject flow) {
        this.source = BsonUtils.getString(flow,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_SOURCE);
        this.dest = BsonUtils.getString(flow, FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_DEST);
    }

    public ScmFlow(String source, String dest) {
        this.source = source;
        this.dest = dest;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_SOURCE).append(":")
                .append(getSource()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_DEST).append(":")
                .append(getDest());
        return sb.toString();
    }

    public BSONObject toBSONObj() {
        BSONObject flow = new BasicBSONObject();
        flow.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_SOURCE, source);
        flow.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_DEST, dest);
        return flow;
    }
}
