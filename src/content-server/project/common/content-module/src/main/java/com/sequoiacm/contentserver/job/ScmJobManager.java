package com.sequoiacm.contentserver.job;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.exception.ScmError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.ServiceDefine;

public class ScmJobManager {
    private static final Logger logger = LoggerFactory.getLogger(ScmJobManager.class);

    private ScheduledExecutorService jobTimer = Executors.newScheduledThreadPool(10);
    private ScmLogResourceJob logResourceJob = new ScmLogResourceJob();
    private static ScmJobManager jobManager = null;

    private ScmJobManager() {
    }

    public static ScmJobManager getInstance() {
        if (jobManager == null) {
            synchronized (ScmJobManager.class) {
                if (jobManager == null) {
                    jobManager = new ScmJobManager();
                }
            }
        }

        return jobManager;
    }

    public void cancel() {
        jobTimer.shutdown();
    }

    public void startLogResourceJob() throws ScmServerException {
        schedule(logResourceJob, ServiceDefine.Job.TRANS_LOG_RESOURCE_DELAY);
    }

    /*
     * @param delay(ms)
     */
    public void schedule(ScmBackgroundJob task, long delay) throws ScmServerException {
        try {
            long period = task.getPeriod();
            if (period > 0) {
                jobTimer.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
            }
            else {
                jobTimer.schedule(task, delay, TimeUnit.MILLISECONDS);
            }

            logger.debug("start BackgroundJob success:type=" + task.getType() + ",name="
                    + task.getName() + ",period=" + task.getPeriod());
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "start background job failed:type=" + task.getType()
                    + ",name=" + task.getName(), e);
        }
    }

    public static void main(String[] args) throws ScmServerException, InterruptedException, UnknownHostException {
        ScmJobManager sjm = ScmJobManager.getInstance();
        ScmBackgroundJob job1 = new ScmBackgroundJob() {
            @Override
            public int getType() {
                return 0;
            }

            @Override
            public long getPeriod() {
                return 2000;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void _run() {
                System.out.println(new Date() + " 1111");
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        ScmBackgroundJob job2 = new ScmBackgroundJob() {
            @Override
            public int getType() {
                return 0;
            }

            @Override
            public long getPeriod() {
                return 4000;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void _run() {
                System.out.println(new Date() + " 2222");
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        sjm.schedule(job1, 1000);
        sjm.schedule(job2, 1000);
        Thread.sleep(5000000);
    }
}
