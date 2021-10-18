package com.sequoiacm.test.config;

import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;

import java.io.File;

public class RunTestLogFileRollingPolicy extends FixedWindowRollingPolicy {

    public RunTestLogFileRollingPolicy() {
        super.setFileNamePattern(LocalPathConfig.BASE_PATH + "log" + File.separator + "runtest.%i.log");
    }

    @Override
    public void setFileNamePattern(String fnp) {
        if (fnp == null || fnp.trim().length() == 0) {
            return;
        }
        super.setFileNamePattern(fnp);
    }
}
