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
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.sequoiacm.schedule.ScheduleApplicationConfig;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.core.job.InternalScheduleInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.core.job.ScheduleMgr;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import com.sequoiacm.schedule.remote.WorkerClient;

public class QuartzScheduleMgr implements ScheduleMgr {
    private static final Logger logger = LoggerFactory.getLogger(QuartzScheduleMgr.class);

    Scheduler sch;
    private Map<String, ScheduleJobInfo> createdJobInfos = new HashMap<>();

    private ScheduleApplicationConfig config;

    private ScheduleClientFactory feignClientFactory;

    private DiscoveryClient discoveryClient;

    private ScheduleDao scheduleDao;

    public QuartzScheduleMgr(ScheduleApplicationConfig config, ScheduleClientFactory clientFactory,
            DiscoveryClient discoveryClient, ScheduleDao scheduleDao) throws Exception {
        sch = StdSchedulerFactory.getDefaultScheduler();
        this.config = config;
        this.discoveryClient = discoveryClient;
        this.feignClientFactory = clientFactory;
        this.scheduleDao = scheduleDao;
    }

    @Override
    public void createJob(ScheduleJobInfo info) throws Exception {
        JobDetail detail = null;
        TriggerBuilder<Trigger> tgBuilder = TriggerBuilder.newTrigger();
        JobKey jobKey = QuartzScheduleTools.createJobKey(info.getId());
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(FieldName.Schedule.FIELD_SCH_INFO, info);
        if (info.getType().equals(ScheduleDefine.ScheduleType.COPY_FILE)) {
            detail = QuartzScheduleTools.createJobDetail(QuartzCopyJob.class, dataMap, jobKey);
            tgBuilder.forJob(detail).withSchedule(CronScheduleBuilder.cronSchedule(info.getCron()));
        }
        else if (info.getType().equals(ScheduleDefine.ScheduleType.CLEAN_FILE)) {
            detail = QuartzScheduleTools.createJobDetail(QuartzCleanJob.class, dataMap, jobKey);
            tgBuilder.forJob(detail).withSchedule(CronScheduleBuilder.cronSchedule(info.getCron()));
        }
        else if (info.getType().equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            dataMap.put(FieldName.Schedule.FIELD_FEIGN_CLIENT_FACTORY, feignClientFactory);
            dataMap.put(FieldName.Schedule.FIELD_DISCOVER_CLIENT, discoveryClient);
            dataMap.put(FieldName.Schedule.FIELD_SCHEDULE_DAO, scheduleDao);
            
            detail = QuartzScheduleTools.createJobDetail(QuartzInternalSchJob.class, dataMap,
                    jobKey);
            tgBuilder.forJob(detail).withSchedule(SimpleScheduleBuilder
                    .repeatSecondlyForever(config.getInternalSchHealthCheckInterval()));
        }

        Trigger trigger = tgBuilder.build();
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
    public void deleteJob(String id, boolean stopWorker) throws Exception {
        sch.deleteJob(QuartzScheduleTools.createJobKey(id));
        ScheduleJobInfo jobInfo = createdJobInfos.remove(id);
        if (jobInfo == null) {
            return;
        }
        if (stopWorker) {
            stopJobSilence(jobInfo);
        }
    }

    private void stopJobSilence(ScheduleJobInfo jobInfo) {
        if (jobInfo.getType().equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            InternalScheduleInfo internalSchInfo = (InternalScheduleInfo) jobInfo;
            synchronized (internalSchInfo) {
                internalSchInfo.setStop(true);
                if (internalSchInfo.getWorkerNode() == null) {
                    return;
                }
                stopInternSchRemoteJob(internalSchInfo);
            }
        }

    }

    private void stopInternSchRemoteJob(InternalScheduleInfo internalSchInfo) {
        WorkerClient client = feignClientFactory
                .getWorkerClientByNodeUrl(internalSchInfo.getWorkerNode());
        int retryTimes = 3;
        while (retryTimes > 0) {
            try {
                client.stopJob(internalSchInfo.getId());
                return;
            }
            catch (Exception e) {
                logger.warn("failed to stop job in remote:job={}", internalSchInfo, e);
            }
            retryTimes--;
        }
        logger.warn("failed to stop jon in remote:job={}", internalSchInfo);
    }

}
