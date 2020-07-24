package com.sequoiacm.cloud.tools.common;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public interface ScmSysTableCleaner {
    void clean() throws ScmToolsException;
}
