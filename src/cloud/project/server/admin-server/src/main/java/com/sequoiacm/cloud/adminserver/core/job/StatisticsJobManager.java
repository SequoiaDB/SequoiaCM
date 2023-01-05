package com.sequoiacm.cloud.adminserver.core.job;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;


public class StatisticsJobManager {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsJobManager.class);

    private static StatisticsJobManager INSTANCE = new StatisticsJobManager();
    private ScmTimer jobTimer = ScmTimerFactory.createScmTimer();
    private StatisticsTrafficJob trafficJob = new StatisticsTrafficJob();
    private StatisticsFileDeltaJob fileDeltaJob = new StatisticsFileDeltaJob();

    private StatisticsJobManager() {
    }

    public static StatisticsJobManager getInstance() {
        return INSTANCE;
    }

    public void startTrafficJob(Date firstTime, long period) throws StatisticsException {
        schedule(trafficJob, firstTime, period);
    }

    public void startFileDeltaJob(Date firstTime, long period) throws StatisticsException {
        schedule(fileDeltaJob, firstTime, period);
    }

    /*
     * @param delay(ms)
     */
    public void schedule(StatisticsJob job, Date firstTime, long period)
            throws StatisticsException {
        try {
            if (period > 0) {
                jobTimer.schedule(job, firstTime, period);
            }
            else if (firstTime != null) {
                jobTimer.schedule(job, firstTime);
            }
            else {
                jobTimer.schedule(job, 0);
            }

            logger.info("start statistics job success:type=" + job.getType() + ",name="
                    + job.getName() + ",period=" + period + ",firstTime=" + firstTime);
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "start statistics job failed:type=" + job.getType() + ",name=" + job.getName(),
                    e);
        }
    }

}
