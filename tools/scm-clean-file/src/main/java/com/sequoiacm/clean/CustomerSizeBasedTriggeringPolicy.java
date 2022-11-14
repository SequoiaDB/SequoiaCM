package com.sequoiacm.clean;

import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;

import java.io.File;

/*
 * Prevent the workspace empty during cleaning, log time is too short,
 * the log size policy is invalid
 */
public class CustomerSizeBasedTriggeringPolicy extends SizeBasedTriggeringPolicy {
    private volatile boolean isFirstStart = true;
    private FileSize maxFileSize;

    @Override
    public boolean isTriggeringEvent(File activeFile, Object event) {
        if (isFirstStart) {
            isFirstStart = false;
            return (activeFile.length() >= maxFileSize.getSize());
        }
        return super.isTriggeringEvent(activeFile, event);
    }

    @Override
    public void setMaxFileSize(FileSize aMaxFileSize) {
        super.setMaxFileSize(aMaxFileSize);
        this.maxFileSize = aMaxFileSize;
    }
}
