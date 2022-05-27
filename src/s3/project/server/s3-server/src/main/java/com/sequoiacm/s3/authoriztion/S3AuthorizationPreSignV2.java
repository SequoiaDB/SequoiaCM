package com.sequoiacm.s3.authoriztion;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.config.AuthorizationConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class S3AuthorizationPreSignV2 extends S3AuthorizationV2 {
    private static final Logger logger = LoggerFactory.getLogger(S3AuthorizationPreSignV2.class);

    private final String expires;

    // http://192.168.30.34:8002/test/test_key3?
    // AWSAccessKeyId=LEP355NZDQQ4XK1HYLI7&
    // Expires=1649670725&
    // Signature=vMyc46xDzGCW8jlzJ4RUu6U3A10%3D
    public S3AuthorizationPreSignV2(HttpServletRequest req, AuthorizationConfig authConfig) throws S3ServerException {
        super(authConfig);
        expires = req.getParameter(RestParamDefine.SignatureV2.EXPIRES);
        accesskey = req.getParameter(RestParamDefine.SignatureV2.ACCESS_KEYID);
        signature = req.getParameter(RestParamDefine.SignatureV2.SIGNATURE);
        if (null == expires || null == accesskey || null == signature) {
            throw new S3ServerException(S3Error.PRE_URL_V2_NEED_QUERY_PARAMETERS,
                    "Query-string authentication requires the Signature, X-Amz-Date, Expires and AWSAccessKeyId parameters. Signature"
                            + signature + ", expires:" + expires + ", AccessKey:" + accesskey);
        }

        String canonicalStr = makeS3CanonicalString(req.getMethod(), req.getRequestURI(), req);
        stringToSign.add(canonicalStr);
    }

    @Override
    protected String buildCanonicalAmzHeaderString(HttpServletRequest request) {
        StringBuilder buf = new StringBuilder();

        // Add all interesting headers to a list, then sort them. "Interesting"
        // is defined as Content-MD5, Content-Type, Date, and x-amz-
        SortedMap<String, String> interestingHeaders = getCanonicalAmzHeaders(request);

        // Remove "x-amz-date" , use expires replace Date
        interestingHeaders.put(DATE, expires);
        interestingHeaders.remove(ALTERNATE_DATE);

        buf.append(buildHeaderString(interestingHeaders));

        return buf.toString();
    }

    @Override
    public void checkExpires(Date serverTime) throws S3ServerException {
        long expiresLong = Long.parseLong(expires);
        if (serverTime.getTime() / 1000 > expiresLong) {
            throw new S3ServerException(S3Error.ACCESS_EXPIRED,
                    "Request has expired. Expires:" + expires + ", ServerTime:" + serverTime);
        }
    }
}
