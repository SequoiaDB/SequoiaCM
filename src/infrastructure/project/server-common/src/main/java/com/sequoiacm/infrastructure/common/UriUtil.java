package com.sequoiacm.infrastructure.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UriUtil {

    private static final String CHARSET_UTF8 = "UTF-8";

    public static String encode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, CHARSET_UTF8).replaceAll("[+]", "%20");
    }

    public static String decode(String source) throws UnsupportedEncodingException {
        return URLDecoder.decode(source, CHARSET_UTF8);
    }
}
