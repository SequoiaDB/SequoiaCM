package com.sequoiacm.diagnose.collect;

import com.sequoiacm.diagnose.common.LogCollectResult;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class LogCollector implements Callable<LogCollectResult> {
    public abstract void start() throws ScmToolsException, IOException;

}
