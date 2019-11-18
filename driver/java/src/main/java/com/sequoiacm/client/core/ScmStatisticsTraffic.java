package com.sequoiacm.client.core;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.client.common.TrafficType;
import com.sequoiacm.common.FieldName;

/**
 * Scm statistics traffic.
 */
public class ScmStatisticsTraffic {

    private TrafficType trafficType;
    private String workspaceName;
    private long traffic;
    private Date recordTime;

    ScmStatisticsTraffic(BSONObject bson) {
        Object obj;
        obj = bson.get(FieldName.Traffic.FIELD_WORKSPACE_NAME);
        if (null != obj) {
            setWorkspaceName((String) obj);
        }
        obj = bson.get(FieldName.Traffic.FIELD_TYPE);
        if (null != obj) {
            setType(TrafficType.getType((String) obj));
        }
        obj = bson.get(FieldName.Traffic.FIELD_TRAFFIC);
        if (null != obj) {
            setTraffic(Long.parseLong(String.valueOf(obj)));
        }
        obj = bson.get(FieldName.Traffic.FIELD_RECORD_TIME);
        if (null != obj) {
            setRecordTime(new Date(Long.parseLong(String.valueOf(obj))));
        }
    }

    void setType(TrafficType type) {
        this.trafficType = type;
    }

    void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    void setTraffic(long traffic) {
        this.traffic = traffic;
    }

    void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

    /**
     * Return the value of the Workspace property name.
     *
     * @return Workspace name
     */
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    /**
     * Returns the traffic type.
     *
     * @return traffic type
     */
    public TrafficType getType() {
        return this.trafficType;
    }

    /**
     * Returns the interface traffic.
     *
     * @return traffic
     */
    public long getTraffic() {
        return this.traffic;
    }

    /**
     * Returns the date represented by this statistical record.
     *
     * @return Record Time
     */
    public Date getRecordTime() {
        return this.recordTime;
    }
}
