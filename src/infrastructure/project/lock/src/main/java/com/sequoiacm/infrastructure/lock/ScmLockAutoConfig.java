package com.sequoiacm.infrastructure.lock;

import org.springframework.context.annotation.Bean;

public class ScmLockAutoConfig {
    @Bean
    public ScmLockManager lockManager(ScmLockConfig c) {
        return new ScmLockManager(c);
    }

    @Bean
    public ScmLockConfig lockConfig() {
        return new ScmLockConfig();
    }
}
