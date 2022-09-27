package com.sequoiacm.schedule.entity;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.common.FieldName;

public class TaskEntityTranslator {
    private static final Logger logger = LoggerFactory.getLogger(TaskEntityTranslator.class);

    public static BSONObject toBSONObject(TaskEntity info) {
        BSONObject obj = new BasicBSONObject();
        obj.put(FieldName.Task.FIELD_ID, info.getId());
        obj.put(FieldName.Task.FIELD_TYPE, info.getType());
        obj.put(FieldName.Task.FIELD_WORKSPACE, info.getWorkspace());
        obj.put(FieldName.Task.FIELD_CONTENT, info.getContent());
        obj.put(FieldName.Task.FIELD_SERVER_ID, info.getServerId());
        obj.put(FieldName.Task.FIELD_PROGRESS, info.getProgress());
        obj.put(FieldName.Task.FIELD_RUNNING_FLAG, info.getRunningFlag());
        obj.put(FieldName.Task.FIELD_START_TIME, info.getStartTime());
        obj.put(FieldName.Task.FIELD_STOP_TIME, info.getStopTime());
        obj.put(FieldName.Task.FIELD_ESTIMATE_COUNT, info.getEstimateCount());
        obj.put(FieldName.Task.FIELD_ACTUAL_COUNT, info.getActualCount());
        obj.put(FieldName.Task.FIELD_SUCCESS_COUNT, info.getSuccessCount());
        obj.put(FieldName.Task.FIELD_FAIL_COUNT, info.getFailCount());
        obj.put(FieldName.Task.FIELD_SCOPE, info.getScope());
        obj.put(FieldName.Task.FIELD_MAX_EXEC_TIME, info.getMaxExecTime());

        String scheduleId = info.getScheduleId();
        if (null != scheduleId) {
            obj.put(FieldName.Task.FIELD_SCHEDULE_ID, scheduleId);
        }

        Integer targetSite = info.getTargetSite();
        if (null != targetSite) {
            obj.put(FieldName.Task.FIELD_TARGET_SITE, targetSite.intValue());
        }
        BSONObject option = info.getOption();
        if (option != null) {
            obj.put(FieldName.Task.FIELD_OPTION, option);
        }
        BSONObject extraInfo = info.getExtraInfo();
        if (extraInfo != null) {
            obj.put(FieldName.Task.FIELD_EXTRA_INFO, extraInfo);
        }

        return obj;
    }

    public static TaskEntity fromBSONObject(BSONObject obj) throws Exception {
        TaskEntity info = new TaskEntity();
        try {
            info.setId((String) obj.get(FieldName.Task.FIELD_ID));
            info.setType((int) obj.get(FieldName.Task.FIELD_TYPE));
            info.setWorkspace((String) obj.get(FieldName.Task.FIELD_WORKSPACE));
            info.setContent((BSONObject) obj.get(FieldName.Task.FIELD_CONTENT));
            info.setServerId((int) obj.get(FieldName.Task.FIELD_SERVER_ID));
            info.setProgress((int) obj.get(FieldName.Task.FIELD_PROGRESS));
            info.setRunningFlag((int) obj.get(FieldName.Task.FIELD_RUNNING_FLAG));
            info.setStartTime((long) obj.get(FieldName.Task.FIELD_START_TIME));
            info.setStopTime(
                    BsonUtils.getNumberOrElse(obj, FieldName.Task.FIELD_STOP_TIME, -1).longValue());
            info.setEstimateCount((long) obj.get(FieldName.Task.FIELD_ESTIMATE_COUNT));
            info.setActualCount((long) obj.get(FieldName.Task.FIELD_ACTUAL_COUNT));
            info.setSuccessCount((long) obj.get(FieldName.Task.FIELD_SUCCESS_COUNT));
            info.setFailCount((long) obj.get(FieldName.Task.FIELD_FAIL_COUNT));

            Object scheduleId = obj.get(FieldName.Task.FIELD_SCHEDULE_ID);
            if (null != scheduleId) {
                info.setScheduleId((String) scheduleId);
            }
            Object targetSite = obj.get(FieldName.Task.FIELD_TARGET_SITE);
            if (null != targetSite) {
                info.setTargetSite((int) targetSite);
            }
        }
        catch (Exception e) {
            logger.error("translate BSONObject to TaskInfo failed:obj={}", obj);
            throw e;
        }

        return info;
    }
}
