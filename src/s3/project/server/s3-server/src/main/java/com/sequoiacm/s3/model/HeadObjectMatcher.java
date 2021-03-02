package com.sequoiacm.s3.model;

import com.sequoiacm.s3.common.RestParamDefine;

import javax.servlet.http.HttpServletRequest;

public class HeadObjectMatcher extends ObjectMatcher {

    public HeadObjectMatcher(HttpServletRequest req) {
        super(req.getHeader(RestParamDefine.HeadObjectHeader.IF_MATCH),
                req.getHeader(RestParamDefine.HeadObjectHeader.IF_NONE_MATCH),
                req.getHeader(RestParamDefine.HeadObjectHeader.IF_MODIFIED_SINCE),
                req.getHeader(RestParamDefine.HeadObjectHeader.IF_UNMODIFIED_SINCE));
    }

}
