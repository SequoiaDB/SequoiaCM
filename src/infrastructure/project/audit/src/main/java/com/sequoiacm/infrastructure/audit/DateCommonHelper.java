package com.sequoiacm.infrastructure.audit;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.bson.types.BSONTimestamp;


public class DateCommonHelper {
    private static SimpleDateFormat ymDateFormat = new SimpleDateFormat("yyyyMM");

    public static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");
        return formatter.format(date);
    }

    public static String getCurrentYearMonth(Date date) {
        synchronized (DateCommonHelper.class) {
            return ymDateFormat.format(date);
        }
    }

    public static String getNextYearMonth(Date date) {
        synchronized (DateCommonHelper.class) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH, 1);
            date = calendar.getTime();
            return ymDateFormat.format(date);
        }
    }

    public static Date getDate(long date) {
        return new Date(date);
    }
    
    public static Date getDate(String date) {
        
        ParsePosition pos = new ParsePosition(0);
        Date parse = ymDateFormat.parse(date, pos );
        return parse;
    }

    public static Date getDate(BSONTimestamp ts) {
        int seconds = ts.getTime();
        int inc = ts.getInc();

        long ms = (long) seconds * 1000 + inc / 1000;
        return new Date(ms);
    }
}
