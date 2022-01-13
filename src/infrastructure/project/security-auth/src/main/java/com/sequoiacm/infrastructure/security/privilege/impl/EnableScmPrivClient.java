package com.sequoiacm.infrastructure.security.privilege.impl;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ScmPrivAutoConfig.class)
public @interface EnableScmPrivClient {
}
