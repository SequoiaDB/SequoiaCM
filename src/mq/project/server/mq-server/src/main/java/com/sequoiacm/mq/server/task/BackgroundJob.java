package com.sequoiacm.mq.server.task;

import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;

public abstract class BackgroundJob extends ScmTimerTask {
    public abstract String getJobName();

    public abstract long getPeriod();
}
