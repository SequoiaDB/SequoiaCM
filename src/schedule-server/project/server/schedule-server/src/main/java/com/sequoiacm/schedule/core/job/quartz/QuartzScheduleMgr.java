package com.sequoiacm.schedule.core.job.quartz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.core.job.CleanJobInfo;
import com.sequoiacm.schedule.core.job.CopyJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleMgr;

public class QuartzScheduleMgr implements ScheduleMgr {
    private static final Logger logger = LoggerFactory.getLogger(QuartzScheduleMgr.class);

    Scheduler sch;
    private Map<String, ScheduleJobInfo> createdJobInfos = new HashMap<>();

    public QuartzScheduleMgr() throws Exception {
        sch = StdSchedulerFactory.getDefaultScheduler();
    }

    @Override
    public void createJob(ScheduleJobInfo info) throws Exception {
        JobDetail detail = null;
        JobKey jobKey = QuartzScheduleTools.createJobKey(info.getId());
        if (info.getType().equals(ScheduleDefine.ScheduleType.COPY_FILE)) {
            CopyJobInfo cInfo = (CopyJobInfo) info;
            JobDataMap dataMap = QuartzScheduleTools.createDataMap(cInfo);
            detail = QuartzScheduleTools.createJobDetail(QuartzCopyJob.class, dataMap, jobKey);
        }
        else {
            CleanJobInfo cInfo = (CleanJobInfo) info;
            JobDataMap dataMap = QuartzScheduleTools.createDataMap(cInfo);
            detail = QuartzScheduleTools.createJobDetail(QuartzCleanJob.class, dataMap, jobKey);
        }

        Trigger trigger = TriggerBuilder.newTrigger().forJob(detail)
                .withSchedule(CronScheduleBuilder.cronSchedule(info.getCron())).build();
        sch.scheduleJob(detail, trigger);
        createdJobInfos.put(info.getId(), info);
    }

    @Override
    public void start() throws Exception {
        sch.start();
    }

    @Override
    public void clear() {
        _clear();
        shutdown();
    }

    private void _clear() {
        try {
            sch.clear();
        }
        catch (Exception e) {
            logger.warn("clear schedule failed", e);
        }
        finally {
            createdJobInfos.clear();
        }
    }

    private void shutdown() {
        try {
            sch.shutdown();
        }
        catch (Exception e) {
            logger.warn("shutdown schedule failed", e);
        }
    }

    @Override
    public ScheduleJobInfo getJobInfo(String id) {
        return createdJobInfos.get(id);
    }

    @Override
    public List<ScheduleJobInfo> ListJob() {
        List<ScheduleJobInfo> jobInfos = new ArrayList<>();
        for (Entry<String, ScheduleJobInfo> jobEntry : createdJobInfos.entrySet()) {
            jobInfos.add(jobEntry.getValue());
        }
        return jobInfos;
    }

    @Override
    public void deleteJob(String id) throws Exception {
        sch.deleteJob(QuartzScheduleTools.createJobKey(id));
        createdJobInfos.remove(id);
    }
}
