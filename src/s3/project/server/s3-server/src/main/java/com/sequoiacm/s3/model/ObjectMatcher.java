package com.sequoiacm.s3.model;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.s3.common.RestParamDefine;

public class ObjectMatcher {

    private String ifMatch;
    private String ifNoneMatch;
    private String ifModifiedSince;
    private String ifUnmodifiedSince;

    public ObjectMatcher(String ifMatch, String ifNoneMatch, String ifModifiedSince,
            String ifUnmodifiedSince) {
        this.ifMatch = ifMatch;
        this.ifNoneMatch = ifNoneMatch;
        this.ifModifiedSince = ifModifiedSince;
        this.ifUnmodifiedSince = ifUnmodifiedSince;
    }

    public String getIfMatch() {
        return ifMatch;
    }

    public String getIfModifiedSince() {
        return ifModifiedSince;
    }

    public String getIfNoneMatch() {
        return ifNoneMatch;
    }

    public String getIfUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

}
