package com.sequoiacm.contentserver.remote;

public class DataInfo {
    private long size;

    public DataInfo setSize(long size) {
        this.size = size;
        return this;
    }

    public long getSize() {
        return size;
    }
}
