package com.sequoiacm.s3.model;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.s3.common.RestParamDefine;

public class CopyObjectMatcher extends ObjectMatcher {

    public CopyObjectMatcher(HttpServletRequest req) {
        super(req.getHeader(RestParamDefine.CopyObjectHeader.IF_MATCH),
                req.getHeader(RestParamDefine.CopyObjectHeader.IF_NONE_MATCH),
                req.getHeader(RestParamDefine.CopyObjectHeader.IF_MODIFIED_SINCE),
                req.getHeader(RestParamDefine.CopyObjectHeader.IF_UNMODIFIED_SINCE));
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
