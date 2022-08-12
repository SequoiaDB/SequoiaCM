package com.sequoiacm.s3import.common.convertor;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class LongParser implements ValueParser {
    private long min;
    private long max;

    public LongParser(long min, long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Object convert(String argName, String val) throws ScmToolsException {
        long longVal = 0;
        try {
            longVal = Long.parseLong(val);
            if (longVal < min || longVal > max) {
                throw new Exception();
            }
        }
        catch (Exception e) {
            throw new ScmToolsException(argName
                    + " out of range. valid range: [" + getMinStr(min) + " , " + getMaxStr(max) + "]: "
                    + val, ScmBaseExitCode.INVALID_ARG);
        }
        return longVal;
    }

    public String getMinStr(long min) {
        if (min == Long.MIN_VALUE) {
            return "LONG_MIN";
        }
        return "" + min;
    }

    public String getMaxStr(long max) {
        if (max == Long.MAX_VALUE) {
            return "LONG_MAX";
        }
        return "" + max;
    }
}
