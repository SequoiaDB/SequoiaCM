package com.sequoiacm.om.omserver.module;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmStatisticsInfo {
    @JsonProperty("data")
    private long data;

    @JsonProperty("record_time")
    private Date recordTime;

    public OmStatisticsInfo() {
    }

    public OmStatisticsInfo(long data, Date recordTime) {
        super();
        this.data = data;
        this.recordTime = recordTime;
    }

    public long getData() {
        return data;
    }

    public void setData(long data) {
        this.data = data;
    }

    public Date getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

}
