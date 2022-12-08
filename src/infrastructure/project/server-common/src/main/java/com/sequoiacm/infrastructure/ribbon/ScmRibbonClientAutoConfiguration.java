package com.sequoiacm.infrastructure.ribbon;

import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@RibbonClients(defaultConfiguration = ScmRibbonClientConfiguration.class)
public class ScmRibbonClientAutoConfiguration {
}
