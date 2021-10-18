package com.sequoiacm.test.config;

import ch.qos.logback.core.rolling.RollingFileAppender;

import java.io.File;

public class RunTestLogFileAppender<E> extends RollingFileAppender<E> {

    public RunTestLogFileAppender() {
        super.setFile(LocalPathConfig.BASE_PATH + "log" + File.separator + "scmtest.log");
    }

    @Override
    public void setFile(String file) {
        if (file == null || file.trim().length() == 0) {
            return;
        }
        super.setFile(file);
    }
}
