package com.sequoiacm.mq.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.sequoiacm.mq.client.config.ConsumerConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ ConsumerConfig.class})
public @interface EnableScmMqConsumer {

}
