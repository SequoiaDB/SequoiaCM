package com.sequoiacm.infrastructure.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnProperty(prefix = "eureka.client", name = "register-with-eureka", havingValue = "true")
public @interface ConditionalOnDiscoveryClient {
}
