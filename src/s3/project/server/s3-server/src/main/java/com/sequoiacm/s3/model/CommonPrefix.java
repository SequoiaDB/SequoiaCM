package com.sequoiacm.s3.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class CommonPrefix {
    @JsonProperty("Prefix")
    private String prefix;

    public CommonPrefix(String prefix, String encodingType) throws S3ServerException {
        try {
            if (null != encodingType) {
                this.prefix = URLEncoder.encode(prefix, "UTF-8");
            }
            else {
                this.prefix = prefix;
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "URL encode failed:" + prefix, e);
        }
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
