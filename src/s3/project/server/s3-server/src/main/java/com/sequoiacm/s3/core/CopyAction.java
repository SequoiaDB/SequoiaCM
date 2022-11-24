package com.sequoiacm.s3.core;

import java.util.ArrayList;
import java.util.List;

public class CopyAction {

    private List<CopyPartInfo> copyList = new ArrayList<>();
    private String baseDataId = null;
    private long baseDataCreateTime;
    private long completeSize = 0;

    public void addCopyInfo(int partNum, String srcDataID, long srcDataCreateTime, long offset,
            long size) {
        copyList.add(new CopyPartInfo(partNum, srcDataID, srcDataCreateTime, offset, size));
    }

    public List<CopyPartInfo> getCopyList() {
        return copyList;
    }

    public void setBaseDataId(String baseDataId) {
        this.baseDataId = baseDataId;
    }

    public String getBaseDataId() {
        return baseDataId;
    }

    public void setBaseDataCreateTime(long baseDataCreateTime) {
        this.baseDataCreateTime = baseDataCreateTime;
    }

    public long getBaseDataCreateTime() {
        return baseDataCreateTime;
    }

    public void setCompleteSize(long completeSize) {
        this.completeSize = completeSize;
    }

    public long getCompleteSize() {
        return completeSize;
    }
}
