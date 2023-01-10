package com.sequoiacm.schedule.common;

import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;
import com.sequoiacm.schedule.common.model.TransitionScheduleEntity;
import com.sequoiacm.schedule.common.model.TransitionUserEntity;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

import java.util.Date;

public class LifeCycleCommonTools {

    public static BSONObject toBSONObject(String key, String content) throws ScheduleException {
        BSONObject contentObj = null;
        try {
            contentObj = (BSONObject) JSON.parse(content);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    key + " is not a valid BSONObject: " + content);
        }

        return contentObj;
    }

    public static ScheduleUserEntity createScheduleUserEntity(String type,
            TransitionScheduleEntity entity, String source, String dest, String preferredRegion,
            String preferredZone, Date date) throws ScheduleException {
        ScheduleUserEntity info = new ScheduleUserEntity();
        info.setWorkspace(entity.getWorkspace());
        info.setPreferredRegion(preferredRegion);
        info.setPreferredZone(preferredZone);
        info.setName(generatedName(entity.getWorkspace(), entity.getTransition().getName(), type,
                date.getTime()));
        info.setType(type);
        info.setEnable(entity.isEnable());
        info.setTransitionId(entity.getId());
        if (type.equals(ScheduleDefine.ScheduleType.MOVE_FILE)) {
            info.setDesc("transition move file schedule");
            info.setCron(entity.getTransition().getTransitionTriggers().getRule());
            BSONObject content = createMoveCopyContent(entity.getTransition(), source, dest);
            info.setContent(content);
        }
        else if (type.equals(ScheduleDefine.ScheduleType.COPY_FILE)) {
            info.setDesc("transition copy file schedule");
            info.setCron(entity.getTransition().getTransitionTriggers().getRule());
            BSONObject content = createMoveCopyContent(entity.getTransition(), source, dest);
            info.setContent(content);
        }
        else if (type.equals(ScheduleDefine.ScheduleType.CLEAN_FILE)) {
            info.setDesc("transition clean file schedule");
            info.setCron(entity.getTransition().getCleanTriggers().getRule());

            BSONObject content = createCleanContent(entity.getTransition(), source, dest);
            info.setContent(content);
        }
        else {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "can not create schedule, unsupport task type, taskType=" + type);
        }
        return info;
    }

    private static BSONObject createCleanContent(TransitionUserEntity transition, String source,
            String dest) throws ScheduleException {
        BSONObject content = getCommonContent(transition);

        content.put(FieldName.Schedule.FIELD_CLEAN_SITE, source);
        content.put(FieldName.Schedule.FIELD_CLEAN_CHECK_SITE, dest);

        BSONObject extraCondition = transition.getMatcher();
        if (extraCondition != null) {
            content.put(FieldName.Schedule.FIELD_EXTRA_CONDITION, extraCondition);
        }

        long maxExecTime = transition.getCleanTriggers().getMaxExecTime();
        content.put(FieldName.Schedule.FIELD_MAX_EXEC_TIME, maxExecTime);

        content.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS,
                transition.getCleanTriggers().toBSONObj());
        return content;
    }

    private static BSONObject createMoveCopyContent(TransitionUserEntity transition, String source,
            String dest) throws ScheduleException {
        BSONObject content = getCommonContent(transition);

        content.put(FieldName.Schedule.FIELD_COPY_SOURCE_SITE, source);
        content.put(FieldName.Schedule.FIELD_COPY_TARGET_SITE, dest);

        BSONObject extraCondition = transition.getMatcher();
        if (extraCondition != null) {
            content.put(FieldName.Schedule.FIELD_EXTRA_CONDITION, extraCondition);
        }

        content.put(FieldName.Schedule.FIELD_MAX_EXEC_TIME,
                transition.getTransitionTriggers().getMaxExecTime());

        content.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS,
                transition.getTransitionTriggers().toBSONObj());
        return content;
    }

    private static BSONObject getCommonContent(TransitionUserEntity transition) throws ScheduleException {
        BSONObject content = new BasicBSONObject();
        String dataCheckLevel = transition.getExtraContent().getDataCheckLevel();
        content.put(FieldName.Schedule.FIELD_DATA_CHECK_LEVEL, dataCheckLevel);

        boolean recycleSpace = transition.getExtraContent().isRecycleSpace();
        content.put(FieldName.Schedule.FIELD_IS_RECYCLE_SPACE, recycleSpace);

        boolean quickStart = transition.getExtraContent().isQuickStart();
        content.put(FieldName.Schedule.FIELD_QUICK_START, quickStart);

        String maxStayTime = "0d";
        content.put(FieldName.Schedule.FIELD_MAX_STAY_TIME, maxStayTime);

        String scope = transition.getExtraContent().getScope();
        content.put(FieldName.Schedule.FIELD_SCOPE, getScopeType(scope));

        return content;
    }

    public static int getScopeType(String scope) throws ScheduleException {
        if (scope.equals("ALL")) {
            return ScheduleDefine.ScopeType.ALL;
        }
        else if (scope.equals("HISTORY")) {
            return ScheduleDefine.ScopeType.HISTORY;
        }
        else if (scope.equals("CURRENT")) {
            return ScheduleDefine.ScopeType.CURRENT;
        }
        else {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "invalid schedule scope type" + scope);
        }
    }

    private static String generatedName(String workspace, String transitionName, String type, long time) {
        return workspace + "_" + transitionName + "_" + type + "_" + time;
    }
}
