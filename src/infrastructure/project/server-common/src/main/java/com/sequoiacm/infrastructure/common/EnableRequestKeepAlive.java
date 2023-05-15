package com.sequoiacm.infrastructure.common;

import com.sequoiacm.infrastructure.config.RequestKeepAliveConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RequestKeepAliveConfiguration.class)
public @interface EnableRequestKeepAlive {

}

@Configuration
@EnableConfigurationProperties(RequestKeepAliveConfig.class)
@EnableAspectJAutoProxy
class RequestKeepAliveConfiguration {

    @Bean
    public RequestKeepAlive requestKeepAlive(RequestKeepAliveConfig requestKeepAliveConfig) {
        return new RequestKeepAlive(requestKeepAliveConfig);
    }

    @Bean
    public RequestKeepAliveAspect requestKeepAliveAspect(RequestKeepAlive requestKeepAlive) {
        return new RequestKeepAliveAspect(requestKeepAlive);
    }

}

@Aspect
class RequestKeepAliveAspect {
    private RequestKeepAlive requestKeepAlive;

    public RequestKeepAliveAspect(RequestKeepAlive requestKeepAlive) {
        this.requestKeepAlive = requestKeepAlive;
    }

    @Around("@annotation(com.sequoiacm.infrastructure.common.KeepAlive)")
    public Object handleKeepAlive(final ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        HttpServletResponse response = requestAttributes.getResponse();
        checkNotNull(request, "request");
        checkNotNull(response, "response");

        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        String keepAliveParameterName = methodSignature.getMethod().getAnnotation(KeepAlive.class)
                .keepAliveParameterName();
        boolean shouldKeepAlive = Boolean.parseBoolean(request.getParameter(keepAliveParameterName));

        if (shouldKeepAlive) {
            ServletOutputStream outputStream = response.getOutputStream();
            Long index = requestKeepAlive.add(outputStream);
            try {
                return pjp.proceed();
            }
            finally {
                requestKeepAlive.remove(index, outputStream);
            }
        }

        return pjp.proceed();
    }

    private void checkNotNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }

}