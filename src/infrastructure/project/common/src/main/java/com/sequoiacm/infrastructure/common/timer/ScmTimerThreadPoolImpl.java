package com.sequoiacm.infrastructure.common.timer;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScmTimerThreadPoolImpl implements ScmTimer {
    private static final Logger logger = LoggerFactory.getLogger(ScmTimerThreadPoolImpl.class);
    private ScheduledExecutorService innerExecutorService = null;

    public ScmTimerThreadPoolImpl() {
        this(1);
    }

    public ScmTimerThreadPoolImpl(int corePoolSize) {
        innerExecutorService = Executors.newScheduledThreadPool(corePoolSize);
    }

    @Override
    public void schedule(ScmTimerTask task, long delayInMillionsecond, long periodInMillionsecond) {
        ScheduledFuture<?> future = innerExecutorService.scheduleAtFixedRate(task,
                delayInMillionsecond, periodInMillionsecond,
                TimeUnit.MILLISECONDS);
        task.setFuture(future);
    }

    @Override
    public void schedule(ScmTimerTask task, long delayInMillionsecond) {
        ScheduledFuture<?> future = innerExecutorService.schedule(task, delayInMillionsecond,
                TimeUnit.MILLISECONDS);
        task.setFuture(future);
    }

    @Override
    public void cancel() {
        try {
            innerExecutorService.shutdown();
        }
        catch (Exception e) {
            logger.warn("failed shutdown ScmTimer", e);
        }
    }

    @Override
    public void schedule(ScmTimerTask task, Date firstTime, long periodInMillionsecond) {
        long now = System.currentTimeMillis();
        long delay = firstTime.getTime() - now;
        if (delay < 0) {
            delay = 0;
        }
        schedule(task, delay, periodInMillionsecond);
    }

    @Override
    public void schedule(ScmTimerTask task, Date firstTime) {
        long now = System.currentTimeMillis();
        long delay = firstTime.getTime() - now;
        if (delay < 0) {
            delay = 0;
        }
        schedule(task, delay);
    }
}
