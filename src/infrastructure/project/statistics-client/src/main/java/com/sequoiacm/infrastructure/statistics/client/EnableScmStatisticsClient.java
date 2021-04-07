package com.sequoiacm.infrastructure.statistics.client;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ ScmStatisticsClientAutoConfig.class })
public @interface EnableScmStatisticsClient {
}
