package com.sequoiacm.s3.authoriztion;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class S3AuthorizationFactory {

    public static S3Authorization createAuthentication(HttpServletRequest webRequest)
            throws S3ServerException {
        String authentication = webRequest.getHeader(S3Authorization.AUTHORIZATION_HEADER);
        if (authentication == null) {
            return null;
        }
        AuthorizationType type = getAuthorizationType(authentication);
        switch (type) {
            case V2:
                try {
                    return new S3AuthorizationV2(webRequest);
                }
                catch (Exception e) {
                    throw new S3ServerException(S3Error.INVALID_AUTHORIZATION,
                            "failed to formate v2 Authorization", e);
                }
            case V4:
                try {
                    return new S3AuthorizationV4(webRequest);
                }
                catch (Exception e) {
                    throw new S3ServerException(S3Error.INVALID_AUTHORIZATION,
                            "failed to formate v4 Authorization", e);
                }
            default:
                throw new S3ServerException(S3Error.INVALID_AUTHORIZATION,
                        "unknown authentication type:" + type);
        }
    }

    private static AuthorizationType getAuthorizationType(String authentication) throws S3ServerException {
        for (AuthorizationType type : AuthorizationType.values()) {
            if (Pattern.matches(type.getRgex(), authentication)) {
                return type;
            }
        }
        throw new S3ServerException(S3Error.INVALID_AUTHORIZATION,
                "unknown authentication type:" + authentication);
    }
}

enum AuthorizationType {
    V2("^AWS .*:.*"),
    V4("^AWS4-HMAC-SHA256.*");

    private String rgex;

    private AuthorizationType(String authenticationHeaderRgex) {
        this.rgex = authenticationHeaderRgex;
    }

    public String getRgex() {
        return rgex;
    }
}
