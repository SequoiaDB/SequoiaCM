package com.sequoiacm.cloud.adminserver.core.job;

import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

public abstract class StatisticsJob extends ScmTimerTask {
    public abstract int getType();
    public abstract String getName();
}
