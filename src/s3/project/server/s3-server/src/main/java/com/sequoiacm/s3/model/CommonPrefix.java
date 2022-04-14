package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.exception.S3ServerException;

public class CommonPrefix {
    @JsonProperty("Prefix")
    private String prefix;

    public CommonPrefix(String prefix, String encodingType) throws S3ServerException {
        this.prefix = S3Codec.encode(prefix, encodingType);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public boolean equals(Object o) {
        CommonPrefix inItem = (CommonPrefix) o;
        return prefix.equals(inItem.prefix);
    }

    @Override
    public int hashCode() {
        return prefix.hashCode();
    }
}
