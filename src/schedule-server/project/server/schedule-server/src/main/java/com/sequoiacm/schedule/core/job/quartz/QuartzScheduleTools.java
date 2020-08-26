package com.sequoiacm.schedule.core.job.quartz;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.core.job.CleanJobInfo;
import com.sequoiacm.schedule.core.job.CopyJobInfo;
import com.sequoiacm.schedule.core.job.InternalScheduleInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.entity.TaskEntity;

class QuartzScheduleTools {
    private static final Logger logger = LoggerFactory.getLogger(QuartzScheduleTools.class);

    private static Lock checkDuplicateTaskLock = new ReentrantLock();

    public static ScheduleJobInfo createJobInfo(JobDataMap dataMap) throws Exception {
        ScheduleJobInfo jobInfo = null;
        String type = dataMap.getString(FieldName.Schedule.FIELD_TYPE);

        if (type.equals(ScheduleDefine.ScheduleType.CLEAN_FILE)) {
            jobInfo = createCleanJobInfo(dataMap);
        }
        else if (type.equals(ScheduleDefine.ScheduleType.COPY_FILE)) {
            jobInfo = createCopyJobInfo(dataMap);
        }
        else if (type.equals(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE)) {
            jobInfo = createInternalJobInfo(dataMap);
        }
        else {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "schedule type is valid:type=" + type);
        }

        return jobInfo;
    }

    private static ScheduleJobInfo createCleanJobInfo(JobDataMap dataMap) throws Exception {
        String id = dataMap.getString(FieldName.Schedule.FIELD_ID);
        String type = dataMap.getString(FieldName.Schedule.FIELD_TYPE);
        String workspace = dataMap.getString(FieldName.Schedule.FIELD_WORKSPACE);
        String cron = dataMap.getString(FieldName.Schedule.FIELD_CRON);
        int days = dataMap.getInt(FieldName.Schedule.FIELD_MAX_STAY_TIME);
        String extra = dataMap.getString(FieldName.Schedule.FIELD_EXTRA_CONDITION);
        BSONObject extraCondition = (BSONObject) JSON.parse(extra);

        int siteId = dataMap.getInt(FieldName.Schedule.FIELD_CLEAN_SITE_ID);
        String siteName = dataMap.getString(FieldName.Schedule.FIELD_CLEAN_SITE);
        int scope = dataMap.getInt(FieldName.Schedule.FIELD_SCOPE);
        long maxExecTime = dataMap.getLong(FieldName.Schedule.FIELD_MAX_EXEC_TIME);
        return new CleanJobInfo(id, type, workspace, siteId, siteName, days, extraCondition, cron,
                scope, maxExecTime);
    }

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

    private static ScheduleJobInfo createCopyJobInfo(JobDataMap dataMap) throws Exception {
        String id = dataMap.getString(FieldName.Schedule.FIELD_ID);
        String type = dataMap.getString(FieldName.Schedule.FIELD_TYPE);
        String workspace = dataMap.getString(FieldName.Schedule.FIELD_WORKSPACE);
        String cron = dataMap.getString(FieldName.Schedule.FIELD_CRON);
        int days = dataMap.getInt(FieldName.Schedule.FIELD_MAX_STAY_TIME);
        String extra = dataMap.getString(FieldName.Schedule.FIELD_EXTRA_CONDITION);
        BSONObject extraCondition = (BSONObject) JSON.parse(extra);

        int sourceSiteId = dataMap.getInt(FieldName.Schedule.FIELD_COPY_SOURCE_SITE_ID);
        String sourceSiteName = dataMap.getString(FieldName.Schedule.FIELD_COPY_SOURCE_SITE);

        int targetSiteId = dataMap.getInt(FieldName.Schedule.FIELD_COPY_TARGET_SITE_ID);
        String targetSiteName = dataMap.getString(FieldName.Schedule.FIELD_COPY_TARGET_SITE);
        int scope = dataMap.getInt(FieldName.Schedule.FIELD_SCOPE);
        long maxExecTime = dataMap.getLong(FieldName.Schedule.FIELD_MAX_EXEC_TIME);
        return new CopyJobInfo(id, type, workspace, sourceSiteId, sourceSiteName, targetSiteId,
                targetSiteName, days, extraCondition, cron, scope, maxExecTime);
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
            int scope, long maxExecTime) {
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
        return task;
    }

    public static BSONObject createDuplicateTaskMatcher(int type, String workspaceName) {
        BSONObject flagList = new BasicBSONList();
        flagList.put("0", ScheduleDefine.TaskRunningFlag.SCM_TASK_INIT);
        flagList.put("1", ScheduleDefine.TaskRunningFlag.SCM_TASK_RUNNING);
        BSONObject inFlag = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_IN, flagList);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.Task.FIELD_WORKSPACE, workspaceName);
        matcher.put(FieldName.Task.FIELD_RUNNING_FLAG, inFlag);

        return matcher;
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

    public static void deleteTask(String taskId) {
        try {
            ScheduleServer.getInstance().deleteTask(taskId);
        }
        catch (Exception e) {
            logger.warn("delete task failed:taskId=" + taskId, e);
        }
    }

    public static Lock getDuplicateTaskLock() {
        return checkDuplicateTaskLock;
    }
}
