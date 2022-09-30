package com.sequoiacm.s3.authoriztion;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.config.AuthorizationConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class S3AuthorizationFactory {

    @Autowired
    private AuthorizationConfig authConfig;

    public S3Authorization createAuthentication(HttpServletRequest webRequest)
            throws S3ServerException {
        try {
            AuthorizationType type = getAuthorizationType(webRequest);
            if (null == type) {
                return null;
            }
            switch (type) {
                case V2:
                    return new S3AuthorizationV2(webRequest, authConfig);
                case V4:
                    return new S3AuthorizationV4(webRequest, authConfig);
                case PRE_SIGN_V2:
                    return new S3AuthorizationPreSignV2(webRequest, authConfig);
                case PRE_SIGN_V4:
                    return new S3AuthorizationPreSignV4(webRequest, authConfig);
                default:
                    throw new S3ServerException(S3Error.INVALID_AUTHORIZATION,
                            "unknown authentication type:" + type);
            }
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.INVALID_AUTHORIZATION, "get authorization failed",
                    e);
        }
    }

    private static AuthorizationType getAuthorizationType(HttpServletRequest webRequest)
            throws S3ServerException {
        for (AuthorizationType type : AuthorizationType.values()) {
            if (type.match(webRequest)) {
                return type;
            }
        }

        return null;
    }
}

enum AuthorizationType {
    V2 {
        @Override
        public boolean match(HttpServletRequest webRequest) {
            return isMatchAuthorizationHeader("^AWS .*:.*", webRequest);
        }
    },
    V4 {
        @Override
        public boolean match(HttpServletRequest webRequest) {
            return isMatchAuthorizationHeader("^AWS4-HMAC-SHA256.*", webRequest);
        }
    },
    PRE_SIGN_V2 {
        @Override
        public boolean match(HttpServletRequest webRequest) {
            return isMatchParameter(RestParamDefine.SignatureV2.SIGNATURE, webRequest);
        }
    },
    PRE_SIGN_V4 {
        @Override
        public boolean match(HttpServletRequest webRequest) {
            return isMatchParameter(RestParamDefine.SignatureV4.X_AMZ_ALGORITHM, webRequest);
        }
    };

    public abstract boolean match(HttpServletRequest webRequest);

    private static boolean isMatchAuthorizationHeader(String regex, HttpServletRequest webRequest) {
        String authString = webRequest.getHeader(S3Authorization.AUTHORIZATION_HEADER);
        if (null != authString && Pattern.matches(regex, authString)) {
            return true;
        }

        return false;
    }

    private static boolean isMatchParameter(String parameterName, HttpServletRequest webRequest) {
        if (webRequest.getParameter(parameterName) != null) {
            return true;
        }

        return false;
    }
}
