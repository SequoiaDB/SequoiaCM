package com.sequoiacm.infrastructure.config.client.cache.bucket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({BucketConfCache.class})
@EnableConfigurationProperties(BucketConfCacheConfig.class)
public @interface EnableBucketCache {
}