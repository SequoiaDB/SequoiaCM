package com.sequoiacm.infrastructure.config.client.core.quota;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(QuotaConfSubscriberAutoConfig.class)
public @interface EnableQuotaSubscriber {
}

class QuotaConfSubscriberAutoConfig {

    @Bean
    public QuotaSubscriberConfig quotaSubscriberConfig() throws ScmConfigException {
        return new QuotaSubscriberConfig();
    }

    @Bean
    public QuotaConfSubscriber quotaConfSubscriber(QuotaSubscriberConfig config,
            ScmConfClient confClient, ApplicationContext applicationContext) {
        return new QuotaConfSubscriber(config, confClient, applicationContext);
    }

}