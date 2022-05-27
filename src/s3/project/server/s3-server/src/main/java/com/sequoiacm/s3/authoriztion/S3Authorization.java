package com.sequoiacm.s3.authoriztion;

import com.sequoiacm.s3.exception.S3ServerException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public abstract class S3Authorization {
    public static final String ISO8601BasicFormat = "yyyyMMdd'T'HHmmss'Z'";
    public static String RFC1123Format = "EEE, dd MMM yyyy HH:mm:ss z";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AMZ_DATE_HEADER = "x-amz-date";
    public static final String X_FORWARDED_PREFIX = "x-forwarded-prefix";

    public abstract List<String> getStringToSign();

    public abstract String getSignature();

    public abstract String getAlgorithm();

    public abstract String getSignatureEncoder();

    public abstract String getAccesskey();

    public abstract String getSecretkeyPrefix();

    public abstract void checkExpires(Date serverTime) throws S3ServerException;

//    String getForwardPrefix(HttpServletRequest req) {
//        String prefix = req.getHeader(X_FORWARDED_PREFIX);
//        if (prefix == null) {
//            return "";
//        }
//        return prefix;
//    }

    public String urlEncode(String url, boolean keepPathSlash) {
        String encoded;
        try {
            encoded = URLEncoder.encode(url, "UTF-8");

            encoded = encoded.replace("+", "%20");
            encoded = encoded.replace("*", "%2A");
            encoded = encoded.replace("%7E", "~");
            if (keepPathSlash) {
                encoded = encoded.replace("%2F", "/");
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported.", e);
        }

        return encoded;
    }
}
