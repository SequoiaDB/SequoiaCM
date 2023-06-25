package com.sequoiacm.infrastructure.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ScmIdParser {
    private ScmParesedId paresedId;

    public ScmIdParser(String id) throws IllegalArgumentException {
        try {
            paresedId = ScmIdGenerator.FileId.parseString(id);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("invalid id string:" + id, e);
        }
    }

    public static String getTimezoneName(String id) {
        return new ScmIdParser(id).getTimezoneType().getTimezoneName();
    }

    public long getSecond() {
        return paresedId.getSeconds();
    }

    public TimezoneType getTimezoneType() {
        TimezoneType type = TimezoneType.getType(paresedId.getTimezoneId());
        if (type == TimezoneType.UNKNOWN) {
            throw new IllegalArgumentException(
                    "cannot get time zone type, invalid timezoneId:" + paresedId.getTimezoneId()
                            + ", id:" + paresedId.getId());
        }
        return type;
    }

    public String getMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        if (getTimezoneType().getTimezoneName() != null) {
            sdf.setTimeZone(TimeZone.getTimeZone(getTimezoneType().getTimezoneName()));
        }
        Date date = new Date((long) paresedId.getSeconds() * 1000);
        return sdf.format(date);
    }

}
