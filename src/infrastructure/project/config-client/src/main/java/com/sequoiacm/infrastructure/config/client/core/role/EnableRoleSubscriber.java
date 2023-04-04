package com.sequoiacm.infrastructure.config.client.core.role;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RoleConfSubscriberAutoConfig.class)
public @interface EnableRoleSubscriber {

}

class RoleConfSubscriberAutoConfig {

    @Bean
    public RoleConfSubscriberConfig roleSubscriberConfig(Environment env) {
        return new RoleConfSubscriberConfig();
    }

    @Bean
    public RoleConfSubscriber roleConfSubscriber(RoleConfSubscriberConfig config,
            ScmConfClient confClient) {
        return new RoleConfSubscriber(config, confClient);
    }

}
