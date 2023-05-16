package com.sequoiacm.infrastructure.sdbversion;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SdbVersionCheckerAutoConfig.class)
public @interface EnableSdbVersionChecker {
    Class<? extends VersionFetcher> versionFetcher();
}
