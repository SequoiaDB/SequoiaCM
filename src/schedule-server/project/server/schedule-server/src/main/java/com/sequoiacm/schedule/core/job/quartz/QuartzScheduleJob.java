package com.sequoiacm.schedule.core.job.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;

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
            info = (ScheduleJobInfo) dataMap.get(FieldName.Schedule.FIELD_SCH_INFO);
            if (info == null) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "schedule info not found in datamap:key="
                                + FieldName.Schedule.FIELD_SCH_INFO + ", datamap=" + dataMap);
            }
            execute(info, context);
        }
        catch (Exception e) {
            logger.warn("execute job failed:info={}", info, e);
            throw new JobExecutionException("execute job failed", e);
        }
    }

    public abstract void execute(ScheduleJobInfo info, JobExecutionContext context)
            throws ScheduleException, JobExecutionException;
}
