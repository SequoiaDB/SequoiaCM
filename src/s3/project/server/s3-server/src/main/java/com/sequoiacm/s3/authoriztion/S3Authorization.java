package com.sequoiacm.s3.authoriztion;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public abstract class S3Authorization {
    public static final String ISO8601BasicFormat = "yyyyMMdd'T'HHmmss'Z'";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AMZ_DATE_HEADER = "x-amz-date";

    public abstract List<String> getStringToSign();

    public abstract String getSignature();

    public abstract String getAlgorithm();

    public abstract String getSignatureEncoder();

    public abstract String getAccesskey();

    public abstract String getSecretkeyPrefix();

    public abstract Date getSignDate();

    // TODO: S3 客户端可能不能填写 gateway:8080/s3 这个样式的地址。可以看下能不能直接从请求中判断是一个s3请求
    String getForwardPrefix(HttpServletRequest req) {
        String prefix = req.getHeader("x-forwarded-prefix");
        if (prefix == null) {
            return "";
        }
        return prefix;
    }
}
