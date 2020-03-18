package com.sequoiacm.cloud.authentication.security;

import com.sequoiacm.infrastructure.security.sign.SignatureInfo;

public class ScmAuthenticationDetail {
    private SignatureInfo signatureInfo;

    public ScmAuthenticationDetail() {
    }

    public void setSignatureInfo(SignatureInfo signatureInfo) {
        this.signatureInfo = signatureInfo;
    }

    public SignatureInfo getSignatureInfo() {
        return signatureInfo;
    }

}
