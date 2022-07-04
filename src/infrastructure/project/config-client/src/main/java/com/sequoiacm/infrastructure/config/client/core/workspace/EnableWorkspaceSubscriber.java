package com.sequoiacm.infrastructure.config.client.core.workspace;

import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketConfSubscriber;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketSubscriberConfig;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.sequoiacm.infrastructure.config.client.EnableConfClient;
import org.springframework.core.env.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(WorkspaceConfSubscriberAutoConfig.class)
public @interface EnableWorkspaceSubscriber {
}

class WorkspaceConfSubscriberAutoConfig {

    @Bean
    public WorkspaceConfSubscriberConfig workspaceConfSubscriberConfig(Environment env)
            throws ScmConfigException {
        return new WorkspaceConfSubscriberConfig(env);
    }

    @Bean
    public WorkspaceConfSubscriber workspaceConfSubscriber(WorkspaceConfSubscriberConfig config,
            ScmConfClient confClient) throws ScmConfigException {
        return new WorkspaceConfSubscriber(config, confClient);
    }

}