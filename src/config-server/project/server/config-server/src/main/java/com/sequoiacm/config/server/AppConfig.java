package com.sequoiacm.config.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.lock.LockConfig;

@Component
public class AppConfig {
    private LockConfig lockConfig;

    public LockConfig getLockConfig() {
        return lockConfig;
    }

    @Autowired
    public void setLockConfig(LockConfig lockConfig) {
        this.lockConfig = lockConfig;
    }

}
