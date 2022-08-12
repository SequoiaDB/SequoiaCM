package com.sequoiacm.s3import.common.convertor;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public interface ValueParser {
    public Object convert(String argName, String val) throws ScmToolsException;
}
