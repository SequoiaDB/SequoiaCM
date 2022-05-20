package com.sequoiacm.om.omserver.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.filter.AuthenticationInterceptor;
import com.sequoiacm.om.omserver.module.BSONObjectConverter;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import com.sequoiacm.om.omserver.session.ScmOmSessionMgr;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private ScmOmSessionMgr sessionMgr;

    @Autowired
    private AuthenticationInterceptor authInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
        argumentResolvers.add(new OmSessionArgumentResolver(sessionMgr));
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        registry.addInterceptor(authInterceptor).excludePathPatterns("/login", "/dock",
                "/internal/v1/health", "/**/error");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new BSONObjectConverter());
    }
}

class OmSessionArgumentResolver implements HandlerMethodArgumentResolver {

    private ScmOmSessionMgr sessionMgr;

    public OmSessionArgumentResolver(ScmOmSessionMgr sessionMgr) {
        this.sessionMgr = sessionMgr;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(ScmOmSession.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String sessionId = webRequest.getHeader(RestParamDefine.X_AUTH_TOKEN);
        ScmOmSession session = sessionMgr.getSession(sessionId);
        if (session == null) {
            throw new ScmOmServerException(ScmOmServerError.SESSION_NOTEXIST,
                    "session not exist:sessionId=" + sessionId);
        }
        return session;
    }
}
