package com.sequoiacm.deploy.common;

import java.io.File;

import com.sequoiacm.deploy.config.CommonConfig;

import ch.qos.logback.core.rolling.RollingFileAppender;

public class DeployLogFileAppender<E> extends RollingFileAppender<E> {
    public DeployLogFileAppender() {
        super.setFile(CommonConfig.getInstance().getBasePath() + File.separator + "deploy.log");
    }

    @Override
    public void setFile(String file) {
        if (file == null || file.trim().length() == 0) {
            return;
        }
        super.setFile(file);
    }
}
