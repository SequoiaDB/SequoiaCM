package com.sequoiacm.s3.core;

public class CopyPartInfo {
    private int partNumber;
    private String srcDataID;
    private long srcDataCreateTime;
    private long destOffset;
    private long partSize;

    public CopyPartInfo(int partNumber, String srcDataID, long srcDataCreateTime, long destOffset,
            long partSize) {
        this.setPartNumber(partNumber);
        this.setSrcDataID(srcDataID);
        this.setDestOffset(destOffset);
        this.setPartSize(partSize);
        this.setSrcDataCreateTime(srcDataCreateTime);
    }

    public long getDestOffset() {
        return destOffset;
    }

    public void setDestOffset(long destOffset) {
        this.destOffset = destOffset;
    }

    public String getSrcDataID() {
        return srcDataID;
    }

    public void setSrcDataID(String srcDataID) {
        this.srcDataID = srcDataID;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public long getPartSize() {
        return partSize;
    }

    public void setSrcDataCreateTime(long srcDataCreateTime) {
        this.srcDataCreateTime = srcDataCreateTime;
    }

    public long getSrcDataCreateTime() {
        return srcDataCreateTime;
    }
}
