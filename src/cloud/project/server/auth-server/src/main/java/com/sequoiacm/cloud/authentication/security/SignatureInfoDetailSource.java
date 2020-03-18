package com.sequoiacm.cloud.authentication.security;

import javax.servlet.http.HttpServletRequest;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.security.authentication.AuthenticationDetailsSource;

import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.security.sign.SignatureInfo;

public class SignatureInfoDetailSource
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
        return detail;
    }

}
