package com.sequoiacm.s3import.common.convertor;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class BooleanParser implements ValueParser {
    @Override
    public Object convert(String argName, String val) throws ScmToolsException {
        if (!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")) {
            throw new ScmToolsException(argName + " out of range. valid range: [true or false]: "
                    + val, ScmBaseExitCode.INVALID_ARG);
        } else return val.equalsIgnoreCase("true");
    }
}
