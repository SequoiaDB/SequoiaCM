package com.sequoiacm.infrastructure.ribbon;

import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@Conditional(ScmRibbonClientAutoConfiguration.LocalPreferredEnabledCondition.class)
@RibbonClients(defaultConfiguration = ScmRibbonClientConfiguration.class)
public class ScmRibbonClientAutoConfiguration {

    public static class LocalPreferredEnabledCondition implements Condition {

        private static final String KEY = "scm.ribbon.localPreferred";

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            if (!context.getEnvironment().containsProperty(KEY)) {
                // 默认开启优先本地调用
                return true;
            }
            String value = context.getEnvironment().getProperty(KEY);
            if ("true".equalsIgnoreCase(value)) {
                return true;
            } else if ("false".equalsIgnoreCase(value)) {
                return false;
            } else {
                throw new IllegalArgumentException("The value of " + KEY + " must be true or false");
            }
        }
    }

}
