package com.sequoiacm.infrastructure.statistics.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public enum ScmTimeAccuracy {
    DAY,
    HOUR;

    public static String truncateTime(String timestamp, ScmTimeAccuracy g)
            throws IllegalArgumentException {
        SimpleDateFormat sdf = new SimpleDateFormat(ScmStatisticsDefine.DATE_PATTERN);
        Date date = null;
        try {
            date = sdf.parse(timestamp);
        }
        catch (ParseException e) {
            throw new IllegalArgumentException("failed to parse timestamp:" + timestamp, e);
        }
        return truncateTime(date.getTime(), g);
    }

    public static String truncateTime(long timestamp, ScmTimeAccuracy g) {
        SimpleDateFormat sdf = new SimpleDateFormat(ScmStatisticsDefine.DATE_PATTERN);

        // Calendar 非单例模式
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(timestamp));
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        if (g == ScmTimeAccuracy.HOUR) {
            return sdf.format(cal.getTime());
        }
        if (g == ScmTimeAccuracy.DAY) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            return sdf.format(cal.getTime());
        }
        throw new IllegalArgumentException("unrecognized time granularity:" + g);
    }
}
