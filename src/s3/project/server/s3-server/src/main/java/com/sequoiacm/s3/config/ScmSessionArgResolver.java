package com.sequoiacm.s3.config;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.infrastructure.security.auth.ScmUserWrapper;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class ScmSessionArgResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(ScmSession.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object session = webRequest.getAttribute(ScmSession.class.getName(),
                NativeWebRequest.SCOPE_REQUEST);
        if (session != null) {
            return session;
        }
        String sessionId = webRequest.getHeader(RestField.SESSION_ATTRIBUTE);
        if (sessionId != null) {
            Object userInfoWrapper = webRequest.getAttribute(RestField.USER_INFO_WRAPPER,
                    NativeWebRequest.SCOPE_REQUEST);
            if (userInfoWrapper != null) {
                return new ScmSession(null, sessionId, (ScmUserWrapper) userInfoWrapper);
            }
            throw new S3ServerException(S3Error.SYSTEM_ERROR,
                    "failed to get user info:" + sessionId);
        }
        throw new S3ServerException(S3Error.INVALID_AUTHORIZATION, "missing authoriztion info");

    }
}
