package com.sequoiacm.cloud.authentication.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequoiacm.infrastructure.security.sign.SignatureInfo;

/**
 * 这是一个存放额外认证信息的类，它的类信息（包括成员变量）会被存在 session 表中。
 * 如果额外认证的信息只是在认证时使用，请加上 @JsonIgnore ，使其不会被序列化和反序列化，避免出现 session 兼容性问题。
 */
public class ScmAuthenticationDetail {
    private SignatureInfo signatureInfo;

    // new version of local user login signature date
    @JsonIgnore
    private String date;

    @JsonIgnore
    private boolean signatureAuthentication;

    public ScmAuthenticationDetail() {
    }

    public void setSignatureInfo(SignatureInfo signatureInfo) {
        this.signatureInfo = signatureInfo;
    }

    public SignatureInfo getSignatureInfo() {
        return signatureInfo;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return this.date;
    }

    public void setSignatureAuthentication(boolean signatureAuthentication) {
        this.signatureAuthentication = signatureAuthentication;
    }

    public boolean getSignatureAuthentication() {
        return this.signatureAuthentication;
    }

}
