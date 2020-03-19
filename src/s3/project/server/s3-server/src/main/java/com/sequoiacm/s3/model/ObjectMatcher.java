package com.sequoiacm.s3.model;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.s3.common.RestParamDefine;

public class ObjectMatcher {

    private String ifMatch;
    private String ifNoneMatch;
    private String ifModifiedSince;
    private String ifUnmodifiedSince;

    public ObjectMatcher(HttpServletRequest req) {
        ifMatch = req.getHeader(RestParamDefine.CopyObjectHeader.IF_MATCH);
        ifNoneMatch = req.getHeader(RestParamDefine.CopyObjectHeader.IF_NONE_MATCH);
        ifModifiedSince = req.getHeader(RestParamDefine.CopyObjectHeader.IF_MODIFIED_SINCE);
        ifUnmodifiedSince = req.getHeader(RestParamDefine.CopyObjectHeader.IF_UNMODIFIED_SINCE);
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
