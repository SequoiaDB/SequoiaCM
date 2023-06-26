package com.sequoiacm.infrastructure.common.timer;

import java.util.Date;

public interface ScmTimer {

    public void schedule(ScmTimerTask task, long delayInMillionsecond, long periodInMillionsecond);

    public void schedule(ScmTimerTask task, long delayInMillionsecond);

    boolean cancelAndAwaitTermination(long timeout) throws InterruptedException;

    public void schedule(ScmTimerTask task, Date firstTime, long periodInMillionsecond);

    public void schedule(ScmTimerTask task, Date firstTime);
    // stop schedule, release timer resource
    public void cancel();

}
