package com.sequoiacm.infrastructure.config.client.core.bucket;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketConfSubscriber;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketSubscriberConfig;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.sequoiacm.infrastructure.config.client.EnableConfClient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(BucketConfSubscriberAutoConfig.class)
public @interface EnableBucketSubscriber {
}

class BucketConfSubscriberAutoConfig {

    @Bean
    public BucketSubscriberConfig bucketSubscriberConfig() throws ScmConfigException {
        return new BucketSubscriberConfig();
    }

    @Bean
    public BucketConfSubscriber bucketConfSubscriber(BucketSubscriberConfig config,
            ScmConfClient confClient, ApplicationContext applicationContext)
            throws ScmConfigException {
        return new BucketConfSubscriber(config, confClient, applicationContext);
    }

}