package com.sequoiacm.infrastructure.sdbversion;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSdbVersion {
    String versionProperty();

    String defaultVersion();
}
