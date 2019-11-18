package com.sequoiacm.deploy.common;

import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

public class DeployConsoleLogFilter extends ThresholdFilter {
    String filterPackage;

    @Override
    public FilterReply decide(ILoggingEvent event) {
        FilterReply ret = super.decide(event);
        String name = event.getLoggerName();
        if (filterPackage == null || !name.startsWith(filterPackage)) {
            return ret;
        }
        return FilterReply.DENY;
    }

    public String getFilterPackage() {
        return filterPackage;
    }

    public void setFilterPackage(String filterPackage) {
        this.filterPackage = filterPackage;
    }
}
