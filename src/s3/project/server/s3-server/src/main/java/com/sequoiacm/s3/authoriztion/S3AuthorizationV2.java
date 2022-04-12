package com.sequoiacm.s3.authoriztion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

public class S3AuthorizationV2 extends S3Authorization {
    private static final Logger logger = LoggerFactory.getLogger(S3AuthorizationV2.class);
    public static final String AMAZON_PREFIX = "x-amz-";
    public static final String ALTERNATE_DATE = "x-amz-date";
    private static final List<String> SIGNED_PARAMETERS = Arrays.asList(new String[] { "acl",
            "torrent", "logging", "location", "policy", "requestPayment", "versioning", "versions",
            "versionId", "notification", "uploadId", "uploads", "partNumber", "website", "delete",
            "lifecycle", "tagging", "cors", "restore", "response-cache-control",
            "response-content-disposition", "response-content-encoding",
            "response-content-language", "response-content-type", "response-expires", });

    private String accesskey;
    private String signature;
    private String algorithm = "HmacSHA1";
    private String secretkeyPrefix = "";
    private Date signDate;
    private List<String> stringToSign = new ArrayList<>();

    public S3AuthorizationV2(HttpServletRequest req) {
        String auth = req.getHeader(S3Authorization.AUTHORIZATION_HEADER);
        // auth:  AWS accesskey:signature
        String[] accesskeyAndSignature = auth.trim().substring("AWS ".length()).split(":");

        accesskey = accesskeyAndSignature[0];
        signature = accesskeyAndSignature[1];
        // gateway:8080/s3/bucket/obj
        
        String canonicalStr = makeS3CanonicalString(req.getMethod(),
                getForwardPrefix(req) + req.getRequestURI(), req);
        stringToSign.add(canonicalStr);

        String dateTimeStamp = req.getHeader(AMZ_DATE_HEADER);
        if (dateTimeStamp != null) {
            signDate = parseAmzDate(dateTimeStamp);
        }
        else {
            long date = req.getDateHeader("date");
            if (date == -1) {
                throw new IllegalArgumentException(
                        "missing date header: date or " + AMZ_DATE_HEADER);
            }
            signDate = new Date(date);
        }

    }

    private Date parseAmzDate(String amzDateStr) {
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
            throw new IllegalArgumentException(
                    "failed to parse " + AMZ_DATE_HEADER + ":" + amzDateStr, e);
        }
    }

    private String makeS3CanonicalString(String method, String resource,
            HttpServletRequest request) {

        StringBuilder buf = new StringBuilder();
        buf.append(method + "\n");

        // Add all interesting headers to a list, then sort them. "Interesting"
        // is defined as Content-MD5, Content-Type, Date, and x-amz-

        Enumeration<String> headerNamesEnum = request.getHeaderNames();
        SortedMap<String, String> interestingHeaders = new TreeMap<String, String>();
        while (headerNamesEnum.hasMoreElements()) {
            String key = headerNamesEnum.nextElement();
            String value = request.getHeader(key);
            String lk = key.toLowerCase(Locale.getDefault());
            // Ignore any headers that are not particularly interesting.
            if (lk.equals("content-type") || lk.equals("content-md5") || lk.equals("date")
                    || lk.startsWith(AMAZON_PREFIX)) {
                interestingHeaders.put(lk, value);
            }
        }

        // Remove default date timestamp if "x-amz-date" is set.
        if (interestingHeaders.containsKey(ALTERNATE_DATE)) {
            interestingHeaders.put("date", "");
        }

        // These headers require that we still put a new line in after them,
        // even if they don't exist.
        if (!interestingHeaders.containsKey("content-type")) {
            interestingHeaders.put("content-type", "");
        }
        if (!interestingHeaders.containsKey("content-md5")) {
            interestingHeaders.put("content-md5", "");
        }

        // Any parameters that are prefixed with "x-amz-" need to be included
        // in the headers section of the canonical string to sign

        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if (parameter.getKey().startsWith("x-amz-")) {
                if (parameter.getValue().length != 1) {
                    throw new IllegalArgumentException("parameter only support one value:"
                            + parameter.getKey() + "=" + parameter.getValue());
                }
                interestingHeaders.put(parameter.getKey(), parameter.getValue()[0]);
            }
        }

        // Add all the interesting headers (i.e.: all that startwith x-amz- ;-))
        for (Iterator<Map.Entry<String, String>> i = interestingHeaders.entrySet().iterator(); i
                .hasNext();) {
            Map.Entry<String, String> entry = i.next();
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith(AMAZON_PREFIX)) {
                buf.append(key).append(':');
                if (value != null) {
                    buf.append(value);
                }
            }
            else if (value != null) {
                buf.append(value);
            }
            buf.append("\n");
        }

        String queryString = request.getQueryString();
        // Add all the interesting parameters
        buf.append(resource);
        String[] parameterNames = request.getParameterMap().keySet()
                .toArray(new String[request.getParameterMap().size()]);
        Arrays.sort(parameterNames);
        char separator = '?';
        for (String parameterName : parameterNames) {
            if (!SIGNED_PARAMETERS.contains(parameterName)) {
                continue;
            }
            buf.append(separator);
            buf.append(parameterName);
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
            }

            separator = '&';
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
    public Date getSignDate() {
        return signDate;
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
