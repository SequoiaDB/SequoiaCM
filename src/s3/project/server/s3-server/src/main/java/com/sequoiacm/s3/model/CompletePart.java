package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompletePart {
    @JsonProperty("PartNumber")
    private int partNumber;
    @JsonProperty("ETag")
    private String etag;

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getEtag() {
        return etag;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public int getPartNumber() {
        return partNumber;
    }
}
