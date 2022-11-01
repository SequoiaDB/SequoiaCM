package com.sequoiacm.cloud.authentication.security;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.cloud.authentication.exception.BadRequestException;
import com.sequoiacm.infrastructure.common.RestCommonDefine;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.security.authentication.AuthenticationDetailsSource;

import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.security.sign.SignatureInfo;
import org.springframework.util.AntPathMatcher;

public class ScmAuthenticationDetailSource
        implements AuthenticationDetailsSource<HttpServletRequest, ScmAuthenticationDetail> {

    @Override
    public ScmAuthenticationDetail buildDetails(HttpServletRequest context) {
        String signJson = context.getParameter(RestField.SIGNATURE_INFO);
        ScmAuthenticationDetail detail = new ScmAuthenticationDetail();
        if (signJson != null) {
            BSONObject signBson = (BSONObject) JSON.parse(signJson);
            SignatureInfo signature = new SignatureInfo(signBson);
            detail.setSignatureInfo(signature);
        }

        AntPathMatcher matcher = new AntPathMatcher();
        if (matcher.match(RestCommonDefine.V2_LOCAL_LOGIN, context.getRequestURI())) {
            String date = context.getHeader(RestField.SIGNATURE_DATE);
            if (date != null) {
                detail.setDate(date);
            }
            else {
                throw new BadRequestException("The date of signature verification cannot be empty");
            }
            detail.setSignatureAuthentication(true);
        }

        return detail;
    }
}
