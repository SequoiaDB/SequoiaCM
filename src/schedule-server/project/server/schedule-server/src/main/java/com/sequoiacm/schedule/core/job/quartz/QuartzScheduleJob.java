package com.sequoiacm.schedule.core.job.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.exception.ScheduleException;

abstract class QuartzScheduleJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(QuartzScheduleJob.class);

    public QuartzScheduleJob() {
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        JobDataMap dataMap = jobDetail.getJobDataMap();

        ScheduleJobInfo info = null;
        try {
            info = QuartzScheduleTools.createJobInfo(dataMap);
            execute(info);
        }
        catch (Exception e) {
            logger.warn("execute job failed:info={}", info, e);
            throw new JobExecutionException("execute job failed", e);
        }
    }

    public abstract void execute(ScheduleJobInfo info) throws ScheduleException;
}
