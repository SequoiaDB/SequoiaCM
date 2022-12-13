package com.sequoiacm.diagnose.collect;

import com.sequoiacm.diagnose.common.CollectResult;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class ClusterCollector implements Callable<CollectResult> {
    public abstract void start() throws ScmToolsException, IOException;
}