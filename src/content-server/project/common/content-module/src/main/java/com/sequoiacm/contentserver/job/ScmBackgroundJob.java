package com.sequoiacm.contentserver.job;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScmBackgroundJob extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ScmBackgroundJob.class);

    /**
     * get the job's type, define in ServiceDefine.job
     *
     * @return the job's type
     */
    public abstract int getType();

    /**
     * get the job's name
     *
     * @return the job's name
     */
    public abstract String getName();

    /**
     * get the job's period time in milliseconds between successive job
     * executions. <=0, just run once; >0, run forever in period.
     *
     * @return the period time(ms)
     */
    public abstract long getPeriod();

    public abstract void _run();

    @Override
    public final void run() {
        try {
            _run();
        }
        catch (Throwable e) {
            logger.error("background job execution failed", e);
        }
    }

    public boolean retryOnThreadPoolReject() {
        return false;
    }

    public long waitingTimeOnReject() {
        // ms, 小于0表示使用默认配置
        return -1;
    }
}
