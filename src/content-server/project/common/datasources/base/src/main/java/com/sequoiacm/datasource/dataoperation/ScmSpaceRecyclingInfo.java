package com.sequoiacm.datasource.dataoperation;

import org.bson.BSONObject;

public class ScmSpaceRecyclingInfo {

    private int successCount;
    private int failedCount;
    private BSONObject info;
    private boolean isTimeout;

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public BSONObject getInfo() {
        return info;
    }

    public void setInfo(BSONObject info) {
        this.info = info;
    }

    public boolean isTimeout() {
        return isTimeout;
    }

    public void setTimeout(boolean timeout) {
        isTimeout = timeout;
    }

    @Override
    public String toString() {
        return "ScmSpaceRecyclingInfo{" + "successCount=" + successCount + ", failedCount="
                + failedCount + ", info=" + info + ", isTimeout=" + isTimeout + '}';
    }
}
