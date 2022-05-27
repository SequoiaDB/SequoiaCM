package com.sequoiacm.infrastructure.common;

import org.apache.http.HttpRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UriUtil {

    private static final String CHARSET_UTF8 = "UTF-8";
    public static final String X_FORWARDED_PREFIX = "x-forwarded-prefix";

    public static String encode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, CHARSET_UTF8).replaceAll("[+]", "%20");
    }

    public static String decode(String source) throws UnsupportedEncodingException {
        return URLDecoder.decode(source, CHARSET_UTF8);
    }

    public static void addForwardPrefix(HttpRequest req, String forwardPrefix) {
        req.addHeader(X_FORWARDED_PREFIX, forwardPrefix);
    }

    public static String getForwardPrefix(HttpServletRequest req) {
        String prefix = req.getHeader(X_FORWARDED_PREFIX);
        if (prefix == null) {
            return "";
        }
        return prefix;
    }
}
