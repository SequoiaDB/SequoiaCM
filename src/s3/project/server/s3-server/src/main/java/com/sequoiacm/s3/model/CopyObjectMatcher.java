package com.sequoiacm.s3.model;

import com.sequoiacm.s3.common.RestParamDefine;

import javax.servlet.http.HttpServletRequest;

public class CopyObjectMatcher extends ObjectMatcher {

    public CopyObjectMatcher(HttpServletRequest req) {
        super(req.getHeader(RestParamDefine.CopyObjectHeader.IF_MATCH),
                req.getHeader(RestParamDefine.CopyObjectHeader.IF_NONE_MATCH),
                req.getHeader(RestParamDefine.CopyObjectHeader.IF_MODIFIED_SINCE),
                req.getHeader(RestParamDefine.CopyObjectHeader.IF_UNMODIFIED_SINCE));
    }

}
