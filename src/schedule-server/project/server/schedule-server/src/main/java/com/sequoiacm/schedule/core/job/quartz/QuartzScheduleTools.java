package com.sequoiacm.schedule.core.job.quartz;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.job.CleanJobInfo;
import com.sequoiacm.schedule.core.job.CopyJobInfo;
import com.sequoiacm.schedule.core.job.InternalScheduleInfo;
import com.sequoiacm.schedule.entity.TaskEntity;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

class QuartzScheduleTools {
    private static final Logger logger = LoggerFactory.getLogger(QuartzScheduleTools.class);

    public static JobDataMap createDataMap(CleanJobInfo info) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(FieldName.Schedule.FIELD_ID, info.getId());
        dataMap.put(FieldName.Schedule.FIELD_TYPE, info.getType());
        dataMap.put(FieldName.Schedule.FIELD_WORKSPACE, info.getWorkspace());
        dataMap.put(FieldName.Schedule.FIELD_CRON, info.getCron());
        dataMap.put(FieldName.Schedule.FIELD_MAX_STAY_TIME, info.getDays());
        dataMap.put(FieldName.Schedule.FIELD_EXTRA_CONDITION, info.getExtraCondtion().toString());

        dataMap.put(FieldName.Schedule.FIELD_CLEAN_SITE_ID, info.getSiteId());
        dataMap.put(FieldName.Schedule.FIELD_CLEAN_SITE, info.getSiteName());
        dataMap.put(FieldName.Schedule.FIELD_SCOPE, info.getScope());
        dataMap.put(FieldName.Schedule.FIELD_MAX_EXEC_TIME, info.getMaxExecTime());
        return dataMap;
    }

    public static JobDataMap createDataMap(InternalScheduleInfo info) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(FieldName.Schedule.FIELD_TYPE, info);
        dataMap.put(FieldName.Schedule.FIELD_INTERNAL_SCH_INFO, info);
        return dataMap;
    }

    private static InternalScheduleInfo createInternalJobInfo(JobDataMap dataMap) throws Exception {
        InternalScheduleInfo info = (InternalScheduleInfo) dataMap
                .get(FieldName.Schedule.FIELD_INTERNAL_SCH_INFO);
        if (info == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "job info not found in datamap:key="
                            + FieldName.Schedule.FIELD_INTERNAL_SCH_INFO + ", datamap=" + dataMap);
        }
        return info;
    }

    public static JobDataMap createDataMap(CopyJobInfo info) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(FieldName.Schedule.FIELD_ID, info.getId());
        dataMap.put(FieldName.Schedule.FIELD_TYPE, info.getType());
        dataMap.put(FieldName.Schedule.FIELD_WORKSPACE, info.getWorkspace());
        dataMap.put(FieldName.Schedule.FIELD_CRON, info.getCron());
        dataMap.put(FieldName.Schedule.FIELD_MAX_STAY_TIME, info.getDays());
        dataMap.put(FieldName.Schedule.FIELD_EXTRA_CONDITION, info.getExtraCondtion().toString());

        dataMap.put(FieldName.Schedule.FIELD_COPY_SOURCE_SITE_ID, info.getSourceSiteId());
        dataMap.put(FieldName.Schedule.FIELD_COPY_SOURCE_SITE, info.getSourceSiteName());

        dataMap.put(FieldName.Schedule.FIELD_COPY_TARGET_SITE_ID, info.getTargetSiteId());
        dataMap.put(FieldName.Schedule.FIELD_COPY_TARGET_SITE, info.getTargetSiteName());

        dataMap.put(FieldName.Schedule.FIELD_SCOPE, info.getScope());
        dataMap.put(FieldName.Schedule.FIELD_MAX_EXEC_TIME, info.getMaxExecTime());
        return dataMap;
    }

    public static JobDetail createJobDetail(Class<? extends Job> jobClass, JobDataMap dataMap,
            JobKey jobKey) {
        JobDetail detail = JobBuilder.newJob(jobClass).setJobData(dataMap).withDescription("")
                .withIdentity(jobKey).build();

        return detail;
    }

    public static JobKey createJobKey(String id) {
        return new JobKey(id);
    }

    public static TaskEntity createTask(int taskType, String taskId, BSONObject taskCondition,
            int serverId, Integer targetSite, long startTime, String workspace, String scheduleId,
            int scope, long maxExecTime, BSONObject option, BSONObject extraInfo) {
        TaskEntity task = new TaskEntity();
        task.setActualCount(0);
        task.setContent(taskCondition);
        task.setEstimateCount(0);
        task.setFailCount(0);
        task.setId(taskId);
        task.setProgress(0);
        task.setRunningFlag(ScheduleDefine.TaskRunningFlag.SCM_TASK_INIT);
        task.setServerId(serverId);
        task.setTargetSite(targetSite);
        task.setStartTime(startTime);
        task.setStopTime(null);
        task.setSuccessCount(0);
        task.setType(taskType);
        task.setWorkspace(workspace);
        task.setScheduleId(scheduleId);
        task.setScope(scope);
        task.setMaxExecTime(maxExecTime);
        task.setOption(option);
        task.setExtraInfo(extraInfo);
        return task;
    }

    public static void closeSilence(Closeable handler) {
        if (null == handler) {
            return;
        }

        try {
            handler.close();
        }
        catch (IOException e) {
            logger.warn("close handle failed:handler={}", handler, e);
        }
    }

    public static void setTaskAbortSilence(String taskId, String msg) {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject modifier = new BasicBSONObject();
            modifier.put(FieldName.Task.FIELD_RUNNING_FLAG,
                    CommonDefine.TaskRunningFlag.SCM_TASK_ABORT);
            modifier.put(FieldName.Task.FIELD_DETAIL, msg);
            ScheduleServer.getInstance().updateTask(matcher, modifier);
        }
        catch (Exception e) {
            logger.warn("Failed to update task running flag to abort:taskId={}, runningFlag={}, detail={}", taskId,
                    CommonDefine.TaskRunningFlag.SCM_TASK_ABORT, msg, e);
        }
    }
}
