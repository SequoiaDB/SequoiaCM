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

    public static void main(String[] args) throws Exception {
        StatisticsJobManager sjm = StatisticsJobManager.getInstance();
        StatisticsJob job1 = new StatisticsJob() {
            @Override
            public int getType() {
                return 0;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void run() {
                System.out.println(new Date() + " 1111");
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        StatisticsJob job2 = new StatisticsJob() {
            @Override
            public int getType() {
                return 0;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    System.out.println(new Date() + " 2222");
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 17);
        instance.set(Calendar.MINUTE, 5);
        instance.set(Calendar.SECOND, 0);
        
        System.out.println(new Date());
        
//        sjm.schedule(job1, instance.getTime(), 10000);
        sjm.schedule(job2, null, 0);
        
        System.out.println("aaaaaaaaaaaa");
//        Thread.sleep(5000000);
    }
}
