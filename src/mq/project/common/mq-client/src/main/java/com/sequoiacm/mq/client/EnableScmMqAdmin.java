package com.sequoiacm.mq.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.sequoiacm.mq.client.config.AdminConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ AdminConfig.class })
public @interface EnableScmMqAdmin {

}
