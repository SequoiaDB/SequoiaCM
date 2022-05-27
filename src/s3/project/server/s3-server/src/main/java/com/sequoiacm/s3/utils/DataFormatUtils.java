package com.sequoiacm.s3.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class  DataFormatUtils {
    public static final String DATA_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String ISO8601BasicFormat = "yyyyMMdd'T'HHmmss'Z'";

    public static String formatISO8601Date(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATA_PATTERN);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(time));
    }

    public static Date parseISO8601Date(String dateTimeStamp) throws S3ServerException {
        Date signDate;
        SimpleDateFormat sdf = new SimpleDateFormat(ISO8601BasicFormat);
        sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
        try {
            signDate = sdf.parse(dateTimeStamp);
        }
        catch (ParseException e) {
            throw new S3ServerException(S3Error.X_AMZ_X_AMZ_DATE_ERROR,
                    "failed to parse " + dateTimeStamp + " to date", e);
        }
        return signDate;
    }
}
