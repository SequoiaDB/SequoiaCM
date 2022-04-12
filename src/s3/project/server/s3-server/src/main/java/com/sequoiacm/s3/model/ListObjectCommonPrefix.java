package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ListObjectCommonPrefix {

    @JsonProperty("Prefix")
    private String prefix;

    public ListObjectCommonPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
