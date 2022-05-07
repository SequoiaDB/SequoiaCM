package com.sequoiacm.s3import.common;

public class ListContext {

    private String nextKeyMarker;

    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    public void setNextKeyMarker(String nextKeyMarker) {
        this.nextKeyMarker = nextKeyMarker;
    }
}
