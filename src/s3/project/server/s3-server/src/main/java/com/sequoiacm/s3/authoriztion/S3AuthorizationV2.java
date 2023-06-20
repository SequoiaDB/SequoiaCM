package com.sequoiacm.s3.authoriztion;

import com.sequoiacm.infrastructure.common.UriUtil;
import com.sequoiacm.s3.config.AuthorizationConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

public class S3AuthorizationV2 extends S3Authorization {
    private static final Logger logger = LoggerFactory.getLogger(S3AuthorizationV2.class);
    public static final String AMAZON_PREFIX = "x-amz-";
    public static final String ALTERNATE_DATE = "x-amz-date";
    public static final String DATE = "date";
    public static final String CONTENT_TYPE = "content-type";
    public static final String CONTENT_MD5 = "content-md5";
    // S3 V2
    // 版本签名计算方法参考：https://docs.aws.amazon.com/zh_cn/general/latest/gr/signature-version-2.html

    // S3 接口中参与 V2 版本签名计算的有值参数列表，其他不在此列表中的有值参数不参与计算
    // 计算签名时，“参数名”、“等号”、“参数值”均参与计算, 如：?versionId=1.0，
    private static final List<String> valuedParameters = Arrays.asList("versionId",
            "uploadId", "partNumber", "response-cache-control", "response-content-disposition",
            "response-content-encoding", "response-content-language", "response-content-type",
            "response-expires");
    private static final Set<String> VALUED_SIGNED_PARAMETERS = new HashSet<>(valuedParameters);
    // S3 接口中参与 V2 版本签名计算的无值参数列表，如：?location
    // 计算签名时，只有“参数名”参与计算，即使url路径中该参数携带“等号”，如：?location=，该“等号”也不参与计算
    private static final List<String> valuelessParameters = Arrays.asList("acl", "torrent", "logging",
            "location", "policy", "requestPayment", "replication", "versioning", "versions",
            "notification", "uploads", "website", "delete", "lifecycle",
            "tagging", "cors", "restore");
    private static final Set<String> VALUELESS_SIGNED_PARAMETERS = new HashSet<>(valuelessParameters);
    protected String accesskey;
    protected String signature;
    protected String algorithm = "HmacSHA1";
    protected String secretkeyPrefix = "";
    private Date signDate;
    protected List<String> stringToSign = new ArrayList<>();
    private AuthorizationConfig authConfig;

    public S3AuthorizationV2(AuthorizationConfig authConfig) {
        this.authConfig = authConfig;
    }

    public S3AuthorizationV2(HttpServletRequest req, AuthorizationConfig authConfig) throws S3ServerException {
        this.authConfig = authConfig;
        String auth = req.getHeader(S3Authorization.AUTHORIZATION_HEADER);
        // auth: AWS accesskey:signature
        String[] accesskeyAndSignature = auth.trim().substring("AWS ".length()).split(":");

        accesskey = accesskeyAndSignature[0];
        signature = accesskeyAndSignature[1];
        // gateway:8080/s3/bucket/obj

        String canonicalStr = makeS3CanonicalString(req.getMethod(),
                UriUtil.getForwardPrefix(req) + req.getRequestURI(), req);
        stringToSign.add(canonicalStr);

        String dateTimeStamp = req.getHeader(AMZ_DATE_HEADER);
        if (dateTimeStamp != null) {
            signDate = parseAmzDate(dateTimeStamp);
        }
        else {
            long date = req.getDateHeader(DATE);
            if (date == -1) {
                throw new S3ServerException(S3Error.ACCESS_NEED_VALID_DATE,
                        "missing date header: date or " + AMZ_DATE_HEADER);
            }
            signDate = new Date(date);
        }
    }

    private Date parseAmzDate(String amzDateStr) throws S3ServerException {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO8601BasicFormat);
        sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
        try {
            return sdf.parse(amzDateStr);
        }
        catch (ParseException e) {
            logger.debug("failed parse amz date: dateStr={}, format={}", amzDateStr,
                    ISO8601BasicFormat, e);
            sdf = new SimpleDateFormat(RFC1123Format, Locale.US);
            sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
            try {
                return sdf.parse(amzDateStr);
            }
            catch (ParseException ex) {
                // 两种时间格式都解析失败了，将两次解析的堆栈进行打印
                logger.error("failed parse amz date: dateStr={}, format={}", amzDateStr,
                        ISO8601BasicFormat, e);
                logger.error("failed parse amz date: dateStr={}, format={}", amzDateStr,
                        RFC1123Format, ex);
            }
            throw new S3ServerException(S3Error.ACCESS_NEED_VALID_DATE,
                    "failed to parse " + AMZ_DATE_HEADER + ":" + amzDateStr, e);
        }
    }

    protected String makeS3CanonicalString(String method, String resource,
            HttpServletRequest request) {

        StringBuilder buf = new StringBuilder();
        buf.append(method + "\n");

        buf.append(buildCanonicalAmzHeaderString(request));

        buf.append(resource);

        buf.append(buildCanonicalizedQueryString(request));

        return buf.toString();
    }

    protected String buildCanonicalAmzHeaderString(HttpServletRequest request) {
        StringBuilder buf = new StringBuilder();

        // Add all interesting headers to a list, then sort them. "Interesting"
        // is defined as Content-MD5, Content-Type, Date, and x-amz-
        SortedMap<String, String> interestingHeaders = getCanonicalAmzHeaders(request);

        // Remove default date timestamp if "x-amz-date" is set.
        if (interestingHeaders.containsKey(ALTERNATE_DATE)) {
            interestingHeaders.put(DATE, "");
        }

        buf.append(buildHeaderString(interestingHeaders));

        return buf.toString();
    }

    protected SortedMap<String, String> getCanonicalAmzHeaders(HttpServletRequest request) {
        Enumeration<String> headerNamesEnum = request.getHeaderNames();
        SortedMap<String, String> interestingHeaders = new TreeMap<>();
        while (headerNamesEnum.hasMoreElements()) {
            String key = headerNamesEnum.nextElement();
            String value = request.getHeader(key);
            String lk = key.toLowerCase(Locale.getDefault());
            // Ignore any headers that are not particularly interesting.
            if (lk.equals(CONTENT_TYPE) || lk.equals(CONTENT_MD5) || lk.equals(DATE)
                    || lk.startsWith(AMAZON_PREFIX)) {
                if (!interestingHeaders.containsKey(lk)) {
                    interestingHeaders.put(lk, value);
                }
                else {
                    // combine header fields with the same name into one
                    // "header-name:comma-separated-value-list" pair.
                    String oldValue = interestingHeaders.get(lk);
                    String newValue = oldValue + "," + value;
                    interestingHeaders.put(lk, newValue);
                }
            }
        }

        if (!interestingHeaders.containsKey(CONTENT_TYPE)) {
            interestingHeaders.put(CONTENT_TYPE, "");
        }
        if (!interestingHeaders.containsKey(CONTENT_MD5)) {
            interestingHeaders.put(CONTENT_MD5, "");
        }

        return interestingHeaders;
    }

    protected String buildHeaderString(
            SortedMap<String, String> interestingHeaders) {
        // Add all the interesting headers
        StringBuilder buf = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> i = interestingHeaders.entrySet().iterator(); i
                .hasNext();) {
            Map.Entry<String, String> entry = i.next();
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith(AMAZON_PREFIX)) {
                buf.append(key).append(':');
            }
            if (value != null) {
                buf.append(value);
            }

            buf.append("\n");
        }
        return buf.toString();
    }

    protected String buildCanonicalizedQueryString(HttpServletRequest request) {
        StringBuilder buf = new StringBuilder();
        String queryString = request.getQueryString();
        String[] parameterNames = request.getParameterMap().keySet()
                .toArray(new String[request.getParameterMap().size()]);
        Arrays.sort(parameterNames);
        char separator = '?';
        for (String parameterName : parameterNames) {
            if (!VALUED_SIGNED_PARAMETERS.contains(parameterName) &&
                    !VALUELESS_SIGNED_PARAMETERS.contains(parameterName)) {
                continue;
            }
            buf.append(separator);
            buf.append(parameterName);
            separator = '&';
            if (VALUELESS_SIGNED_PARAMETERS.contains(parameterName)){
                continue;
            }
            String[] parameterValue = request.getParameterValues(parameterName);
            if (parameterValue != null) {
                if (parameterValue.length != 1) {
                    throw new IllegalArgumentException("parameter only support one value:"
                            + parameterName + "=" + Arrays.toString(parameterValue));
                }

                if (parameterValue[0].isEmpty()) {
                    // 这个 parameter 时没有值的, 这里需要识别这个 parameter 在 url 中的形式
                    // 1. path?parameter
                    // 2. path?parameter=
                    // 通过回查 queryString 确定是否需要拼接 =
                    if (queryString.contains(parameterName + "=")) {
                        buf.append("=").append(parameterValue[0]);
                    }
                }
                else {
                    buf.append("=").append(urlEncode(parameterValue[0], false));
                }
            }
        }

        return buf.toString();
    }

    @Override
    public String getAccesskey() {
        return accesskey;
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

    @Override
    public List<String> getStringToSign() {
        return stringToSign;
    }

    @Override
    public String getSignatureEncoder() {
        return "base64";
    }

    @Override
    public String toString() {
        return "S3AuthorizationV2{" + "accesskey='" + accesskey + '\'' + ", signature='" + signature
                + '\'' + ", algorithm='" + algorithm + '\'' + ", secretkeyPrefix='"
                + secretkeyPrefix + '\'' + ", signDate=" + signDate + ", stringToSign="
                + stringToSign + '}';
    }
}
