package com.sequoiacm.s3.config;

import com.sequoiacm.s3.model.CopyObjectMatcher;
import com.sequoiacm.s3.model.HeadObjectMatcher;
import org.apache.catalina.connector.RequestFacade;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CopyObjArgResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CopyObjectMatcher.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        RequestFacade r = webRequest.getNativeRequest(RequestFacade.class);
        return new CopyObjectMatcher(r);
    }
}
