package com.sequoiacm.s3.core;

public class DataInfo {
    String dataId;
    long createTime;

    public DataInfo(String dataId, long createTime) {
        this.dataId = dataId;
        this.createTime = createTime;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getCreateTime() {
        return createTime;
    }
}
