package com.sequoiacm.s3.authoriztion;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.infrastructure.common.UriUtil;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.s3.config.AuthorizationConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.utils.DataFormatUtils;

public class S3AuthorizationV4 extends S3Authorization {
    public static final String AMZ_HASH_HEADER = "x-amz-content-sha256";
    public static final String CREDENTIAL_PREFIX = "Credential=";
    public static final String SIGNED_HEADER_PREFIX = "SignedHeaders=";
    public static final String SIGNATURE_PREFIX = "Signature=";

    protected String accesskey;
    protected String signature;
    protected String algorithm = "HmacSHA256";
    protected String secretkeyPrefix = "AWS4";
    private Date signDate;
    protected List<String> stringToSign = new ArrayList<>();
    private AuthorizationConfig authConfig;

    public S3AuthorizationV4(AuthorizationConfig authConfig) {
        this.authConfig = authConfig;
    }

    public S3AuthorizationV4(HttpServletRequest req, AuthorizationConfig authConfig)
            throws S3ServerException {
        this.authConfig = authConfig;
        String authorization = req.getHeader(S3Authorization.AUTHORIZATION_HEADER);
        // authorization: AWS4-HMAC-SHA256
        // Credential=N5OP54C5UAFCWECGGOBW/20200414/us-east-1/s3/aws4_request,
        // SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class,
        // Signature=2cd618b15e0a2e85a7c02fb1e7bf183bff0898c29a984318d85b69d799923af6
        AuthorizationInfo authorizationInfo = new AuthorizationInfo(authorization);
        secretkeyPrefix = authorizationInfo.getScheme();
        accesskey = authorizationInfo.getCrendential().getAccesskey();
        signature = authorizationInfo.getSignature();

        String reqToSign = getReqToSign(req, authorizationInfo);

        stringToSign.add(authorizationInfo.getCrendential().getDateStamp());
        stringToSign.add(authorizationInfo.getCrendential().getRegion());
        stringToSign.add(authorizationInfo.getCrendential().getService());
        stringToSign.add(authorizationInfo.getCrendential().getTerminator());
        stringToSign.add(reqToSign);
    }

    private String getReqToSign(HttpServletRequest req, AuthorizationInfo authorizationInfo)
            throws S3ServerException {
        String path = getCanonicalizedResourcePath(req);

        String queryParameters = "";
        if (req.getQueryString() != null) {
            queryParameters = getCanonicalizedQueryString(req.getParameterMap());
        }

        if (authConfig.isSortHeaders()) {
            Arrays.sort(authorizationInfo.getHeaderArray());
            authorizationInfo
                    .setHeaders(buildSignedHeaderNames(authorizationInfo.getHeaderArray()));
        }
        String canonicalizedHeaders = getCanonicalizedHeaderString(req,
                authorizationInfo.getHeaderArray());

        String hash = req.getHeader(AMZ_HASH_HEADER);
        if (hash == null) {
            throw new IllegalArgumentException("missing required header:" + AMZ_HASH_HEADER);
        }
        String canonicalReq = getCanonicalRequest(path, req.getMethod(), queryParameters,
                authorizationInfo.getHeaders(), canonicalizedHeaders, hash);

        String dateTimeStamp = req.getHeader(AMZ_DATE_HEADER);
        if (dateTimeStamp == null) {
            throw new S3ServerException(S3Error.ACCESS_NEED_VALID_DATE,
                    "missing required header:" + AMZ_DATE_HEADER);
        }
        signDate = DataFormatUtils.parseISO8601Date(dateTimeStamp);

        return new StringBuilder().append(authorizationInfo.getScheme()).append("-")
                .append(authorizationInfo.getAlgorithm()).append("\n").append(dateTimeStamp)
                .append("\n").append(authorizationInfo.getCrendential().getScope()).append("\n")
                .append(SignUtil.toHex(SignUtil.hash(canonicalReq))).toString();
    }

    @Override
    public List<String> getStringToSign() {
        return stringToSign;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getAccesskey() {
        return accesskey;
    }

    @Override
    public String getSecretkeyPrefix() {
        return secretkeyPrefix;
    }

    @Override
    public void checkExpires(Date serverTime) throws S3ServerException {
        int maxTimeOffset = authConfig.getMaxTimeOffset();
        if (maxTimeOffset > 0
                && Math.abs(signDate.getTime() - serverTime.getTime()) > maxTimeOffset) {
            throw new S3ServerException(S3Error.REQUEST_TIME_TOO_SKEWED,
                    "request time too skewed:requestTime=" + signDate
                            + ", serverTime=" + serverTime);
        }
    }

    protected String getCanonicalizeHeaderNames(HttpServletRequest req) {
        List<String> sortedHeaders = new ArrayList<String>();
        Enumeration<String> h = req.getHeaderNames();
        while (h.hasMoreElements()) {
            sortedHeaders.add(h.nextElement());
        }
        Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (buffer.length() > 0)
                buffer.append(";");
            buffer.append(header.toLowerCase());
        }

        return buffer.toString();
    }

    protected String buildSignedHeaderNames(String[] sortedHeaders) {
        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (buffer.length() > 0)
                buffer.append(";");
            buffer.append(header.toLowerCase());
        }
        return buffer.toString();
    }

    /**
     * Computes the canonical headers with values for the request. For AWS4, all
     * headers must be included in the signing process.
     */
    protected String getCanonicalizedHeaderString(HttpServletRequest req, String[] headerArr) {
        StringBuilder buffer = new StringBuilder();
        for (String key : headerArr) {
            buffer.append(key.toLowerCase().replaceAll("\\s+", " ")).append(":");
            String value = req.getHeader(key);
            if (value != null) {
                buffer.append(value.replaceAll("\\s+", " "));
            }

            buffer.append("\n");
        }
        return buffer.toString();
    }

    protected String getCanonicalRequest(String path, String httpMethod, String queryParameters,
            String canonicalizedHeaderNames, String canonicalizedHeaders, String bodyHash) {
        return new StringBuilder().append(httpMethod).append("\n").append(path).append("\n")
                .append(queryParameters).append("\n").append(canonicalizedHeaders).append("\n")
                .append(canonicalizedHeaderNames).append("\n").append(bodyHash).toString();
    }

    /**
     * Returns the canonicalized resource path for the service endpoint.
     */
    protected String getCanonicalizedResourcePath(HttpServletRequest req) throws S3ServerException {
        String path = req.getRequestURI();
        try {
            path = URLDecoder.decode(path, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new S3ServerException(S3Error.INVALID_AUTHORIZATION,
                    "UTF-8 decoding is not supported.", e);
        }
        String forwardPrefix = UriUtil.getForwardPrefix(req);
        path = forwardPrefix + path;

        if (path == null || path.isEmpty()) {
            return "/";
        }

        String encodedPath = urlEncode(path, true);
        if (encodedPath.startsWith("/")) {
            return encodedPath;
        }
        else {
            return "/".concat(encodedPath);
        }
    }

    protected String getCanonicalizedQueryString(Map<String, String[]> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        SortedMap<String, String> sorted = new TreeMap<>();

        Iterator<Map.Entry<String, String[]>> pairs = map.entrySet().iterator();
        while (pairs.hasNext()) {
            Map.Entry<String, String[]> pair = pairs.next();
            String key = pair.getKey();
            String[] value = pair.getValue();
            if (value.length > 1) {
                throw new IllegalArgumentException(
                        "multi value for query param:" + key + "=" + Arrays.toString(value));
            }
            if (value.length == 0) {
                throw new IllegalArgumentException("no value for query param:" + key);
            }
            sorted.put(urlEncode(key, false), urlEncode(value[0], false));
        }

        StringBuilder builder = new StringBuilder();
        Iterator<Entry<String, String>> sortedPairs = sorted.entrySet().iterator();
        while (sortedPairs.hasNext()) {
            Map.Entry<String, String> pair = sortedPairs.next();
            builder.append(pair.getKey());
            builder.append("=");
            builder.append(pair.getValue());
            if (sortedPairs.hasNext()) {
                builder.append("&");
            }
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "S3AuthorizationV4 [accesskey=" + accesskey + ", signature=" + signature
                + ", algorithm=" + algorithm + ", secretkeyPrefix=" + secretkeyPrefix
                + ", signDate=" + signDate + ", stringToSign=" + stringToSign + "]";
    }

    @Override
    public String getSignatureEncoder() {
        return "base16";
    }
}

class CredentialInfo {
    private String accesskey;
    private String dateStamp;
    private String region;
    private String service;
    private String terminator;
    private String scope;

    public CredentialInfo(String credential) {
        String[] credentialArr = credential.split("/");
        accesskey = credentialArr[0];
        dateStamp = credentialArr[1];
        region = credentialArr[2];
        service = credentialArr[3];
        terminator = credentialArr[4];
        scope = dateStamp + "/" + region + "/" + service + "/" + terminator;
    }

    public String getScope() {
        return scope;
    }

    public String getTerminator() {
        return terminator;
    }

    public String getAccesskey() {
        return accesskey;
    }

    public String getDateStamp() {
        return dateStamp;
    }

    public String getRegion() {
        return region;
    }

    public String getService() {
        return service;
    }

}

class AuthorizationInfo {
    private String scheme;
    private String algorithm;
    private CredentialInfo crendentialInfo;
    private String signature;
    private String headers;
    private String[] headerArray;

    public AuthorizationInfo(String authorization) {
        String schemeAndAlgorithm = authorization.substring(0, authorization.indexOf(" "));
        scheme = schemeAndAlgorithm.substring(0, schemeAndAlgorithm.indexOf("-"));
        algorithm = schemeAndAlgorithm.substring(scheme.length() + 1);

        String credentialHeadersSignature = authorization.substring(schemeAndAlgorithm.length())
                .trim();
        String[] credentialHeadersSignatureArr = credentialHeadersSignature.split(",");

        String credentialStr = credentialHeadersSignatureArr[0].trim();
        crendentialInfo = new CredentialInfo(
                credentialStr.substring("Credential=".length()).trim());

        String headerStr = credentialHeadersSignatureArr[1].trim();
        headers = headerStr.substring("SignedHeaders=".length());
        headerArray = headers.split(";");

        String signatureStr = credentialHeadersSignatureArr[2].trim();
        signature = signatureStr.substring("Signature=".length());
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public CredentialInfo getCrendential() {
        return crendentialInfo;
    }

    public String getHeaders() {
        return headers;
    }

    public String getScheme() {
        return scheme;
    }

    public String getSignature() {
        return signature;
    }

    public String[] getHeaderArray() {
        return headerArray;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }
}
