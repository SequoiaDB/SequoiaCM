package com.sequoiacm.s3import.module;

public class IgnoreMetadata {

    String srcValue;
    String destValue;

    public IgnoreMetadata(String srcValue, String destValue) {
        this.srcValue = srcValue.equals("null") ? null : srcValue;
        this.destValue = destValue.equals("null") ? null : destValue;
    }

    public String getSrcValue() {
        return srcValue;
    }

    public String getDestValue() {
        return destValue;
    }
}
