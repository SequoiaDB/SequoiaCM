package com.sequoiacm.s3import.common.convertor;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class IntegerParser implements ValueParser {
    private int min;
    private int max;

    public IntegerParser(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Object convert(String argName, String val) throws ScmToolsException {
        int intVal = 0;
        try {
            intVal = Integer.parseInt(val);
            if (intVal < min || intVal > max) {
                throw new Exception();
            }
        }
        catch (Exception e) {
            throw new ScmToolsException(argName
                    + " out of range. valid range: [" + getMinStr(min) + " , " + getMaxStr(max) + "]: "
                    + val, ScmBaseExitCode.INVALID_ARG);
        }
        return intVal;
    }

    private String getMinStr(int min) {
        if (min == Integer.MIN_VALUE) {
            return "INT_MIN";
        }
        return "" + min;
    }

    private String getMaxStr(int max) {
        if (max == Integer.MAX_VALUE) {
            return "INT_MAX";
        }
        return "" + max;
    }
}
