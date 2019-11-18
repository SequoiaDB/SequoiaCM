package com.sequoiacm.deploy.common;

import java.io.File;

import com.sequoiacm.deploy.config.CommonConfig;

import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;

public class DeployLogFileRollingPolicy extends FixedWindowRollingPolicy {
    public DeployLogFileRollingPolicy() {
        super.setFileNamePattern(
                CommonConfig.getInstance().getBasePath() + File.separator + "deploy.%i.log");
    }

    @Override
    public void setFileNamePattern(String fnp) {
        if (fnp == null || fnp.trim().length() == 0) {
            return;
        }
        super.setFileNamePattern(fnp);
    }
}
