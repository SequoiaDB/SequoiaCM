package com.sequoiacm.infrastructure.config.client.core.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(UserConfSubscriberAutoConfig.class)
public @interface EnableUserSubscriber {

}

class UserConfSubscriberAutoConfig {

    @Bean
    public UserConfSubscriberConfig userSubscriberConfig() {
        return new UserConfSubscriberConfig();
    }

    @Bean
    public UserConfSubscriber userConfSubscriber(UserConfSubscriberConfig config,
            ScmConfClient confClient) {
        return new UserConfSubscriber(config, confClient);
    }

}
