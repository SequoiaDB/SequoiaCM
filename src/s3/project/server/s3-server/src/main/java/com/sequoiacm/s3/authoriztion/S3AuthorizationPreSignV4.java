package com.sequoiacm.s3.authoriztion;

import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.config.AuthorizationConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.utils.DataFormatUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class S3AuthorizationPreSignV4 extends S3AuthorizationV4 {
    private final String dateTimeStamp;
    private String signedHeaders;
    private final String xAmzAlgorithm;
    private final String expires;
    private final AuthorizationConfig authConfig;

    // http://192.168.30.34:8002/test/test_key3?
    // X-Amz-Algorithm=AWS4-HMAC-SHA256&
    // X-Amz-Date=20220411T094931Z&
    // X-Amz-SignedHeaders=host&
    // X-Amz-Expires=60&
    // X-Amz-Credential=LEP355NZDQQ4XK1HYLI7/20220411/us-east-1/s3/aws4_request&
    // X-Amz-Signature=a0382c8d16894af91292871d5daff3a5edee8cfe0e7eee9c52a4b7be094ba996
    public S3AuthorizationPreSignV4(HttpServletRequest req, AuthorizationConfig authConfig)
            throws S3ServerException {
        super(authConfig);
        this.authConfig = authConfig;
        xAmzAlgorithm = req.getParameter(RestParamDefine.SignatureV4.X_AMZ_ALGORITHM);
        dateTimeStamp = req.getParameter(RestParamDefine.SignatureV4.X_AMZ_DATE);
        signedHeaders = req.getParameter(RestParamDefine.SignatureV4.X_AMZ_SIGNEDHEADERS);
        String xAmzCredential = req.getParameter(RestParamDefine.SignatureV4.X_AMZ_CREDENTIAL);
        signature = req.getParameter(RestParamDefine.SignatureV4.X_AMZ_SIGNATURE);
        expires = req.getParameter(RestParamDefine.SignatureV4.X_AMZ_EXPIRES);
        if (null == xAmzAlgorithm || null == signature || null == dateTimeStamp
                || null == signedHeaders || null == xAmzCredential || null == expires) {
            throw new S3ServerException(S3Error.PRE_URL_V4_NEED_QUERY_PARAMETERS,
                    " X-Amz-Algorithm, X-Amz-Credential, X-Amz-Signature, X-Amz-Date, "
                            + "X-Amz-SignedHeaders, and X-Amz-Expires parameters can not be null");
        }

        CredentialInfo credentialInfo = new CredentialInfo(xAmzCredential);

        accesskey = credentialInfo.getAccesskey();

        stringToSign.add(credentialInfo.getDateStamp());
        stringToSign.add(credentialInfo.getRegion());
        stringToSign.add(credentialInfo.getService());
        stringToSign.add(credentialInfo.getTerminator());

        String reqToSign = getReqToSign(req, credentialInfo);
        stringToSign.add(reqToSign);
    }

    private String getReqToSign(HttpServletRequest req, CredentialInfo credentialInfo)
            throws S3ServerException {
        String path = getCanonicalizedResourcePath(req);

        String queryParameters = "";
        if (req.getQueryString() != null) {
            Map<String, String[]> map = new HashMap<>(req.getParameterMap());
            map.remove(RestParamDefine.SignatureV4.X_AMZ_SIGNATURE);
            queryParameters = getCanonicalizedQueryString(map);
        }

        String[] headerArray = signedHeaders.split(";");
        if (authConfig.isSortHeaders()) {
            Arrays.sort(headerArray);
            signedHeaders = buildSignedHeaderNames(headerArray);
        }
        String canonicalizedHeaders = getCanonicalizedHeaderString(req, headerArray);

        String canonicalReq = new StringBuilder().append(req.getMethod()).append("\n").append(path)
                .append("\n").append(queryParameters).append("\n").append(canonicalizedHeaders)
                .append("\n").append(signedHeaders).append("\n")
                .append(RestParamDefine.SignatureV4.AMZ_HASH).toString();

        return new StringBuilder().append(xAmzAlgorithm).append("\n").append(dateTimeStamp)
                .append("\n").append(credentialInfo.getScope()).append("\n")
                .append(SignUtil.toHex(SignUtil.hash(canonicalReq))).toString();
    }

    @Override
    public void checkExpires(Date serverTime) throws S3ServerException {
        // must be number
        long expireTime;
        try {
            expireTime = Long.parseLong(expires);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.NUMBER_X_AMZ_EXPIRES,
                    "X-Amz-Expires should be a number");
        }
        // must less than a week
        if (expireTime > RestParamDefine.SignatureV4.X_AMZ_EXPIRES_MAX) {
            throw new S3ServerException(S3Error.X_AMZ_EXPIRES_TOO_LARGE,
                    "X-Amz-Expires must be less than a week (in seconds)");
        }
        // non-negative
        if (expireTime < RestParamDefine.SignatureV4.X_AMZ_EXPIRES_MIN) {
            throw new S3ServerException(S3Error.X_AMZ_EXPIRES_NEGATIVE,
                    "X-Amz-Expires must be non-negative");
        }

        Date xAmzDate = DataFormatUtils.parseISO8601Date(dateTimeStamp);

        if (serverTime.getTime() / 1000 - xAmzDate.getTime() / 1000 > expireTime) {
            throw new S3ServerException(S3Error.ACCESS_EXPIRED, "Request has expired.  X-Amz-Date:"
                    + dateTimeStamp + ", X-Amz-Expires:" + expireTime + ", ServerTime:" + serverTime);
        }
    }
}
