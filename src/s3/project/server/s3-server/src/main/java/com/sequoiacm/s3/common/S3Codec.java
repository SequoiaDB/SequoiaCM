package com.sequoiacm.s3.common;

import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class S3Codec {

    public static String decode(String src, String encodingType) throws S3ServerException {
        if (src == null) {
            return null;
        }
        try {
            if (null != encodingType) {
                return URLDecoder.decode(src, "UTF-8");
            }
            return src;
        }
        catch (UnsupportedEncodingException e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "URL decode failed:" + src, e);
        }
    }

    public static String encode(String src, String encodingType) throws S3ServerException {
        if (src == null) {
            return null;
        }
        try {
            if (null != encodingType) { // AmazonS3当前若encodingType不为空且只能是url这个值,url对应的编码方式是UTF-8
                return URLEncoder.encode(src, "UTF-8");
            }
            return src;
        }
        catch (UnsupportedEncodingException e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "URL encode failed:" + src, e);
        }
    }
}
