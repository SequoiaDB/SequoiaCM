package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.LifeCycleCommonTools;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.RestCommonField;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Date;

public class LifeCycleEntityTranslator {
    private static final Logger logger = LoggerFactory.getLogger(LifeCycleEntityTranslator.class);

    public static class UserInfo {
        public static LifeCycleConfigUserEntity analyzeConfig(String config)
                throws ScheduleException {
            if (!StringUtils.hasText(config)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.MISSING_ARGUMENT,
                        "miss argument:"
                                + RestCommonField.LIFE_CYCLE_CONFIG);
            }
            BSONObject obj = LifeCycleCommonTools.toBSONObject(
                    RestCommonField.LIFE_CYCLE_CONFIG, config);
            return fromBSONObject(obj);
        }

        public static LifeCycleConfigUserEntity fromBSONObject(BSONObject obj) {
            LifeCycleConfigUserEntity info = new LifeCycleConfigUserEntity();
            try {
                info.setStageTagConfig((BasicBSONList) obj.get(
                        RestCommonField.STAGE_TAG_CONFIG));
                info.setTransitionConfig((BasicBSONList) obj.get(
                        RestCommonField.TRANSITION_CONFIG));
            }
            catch (Exception e) {
                logger.error("translate BSONObject to LifeCycleConfigUserEntity failed:obj={}",
                        obj);
                throw e;
            }

            return info;
        }

        public static String toJSONString(LifeCycleConfigUserEntity entity)
                throws ScheduleException {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.writeValueAsString(entity);
            }
            catch (JsonProcessingException e) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "formate LifeCycleConfigUserEntity failed:entity=" + entity, e);
            }
        }

        public static LifeCycleConfigUserEntity fromStageTagConfig(String stageTagName,
                String stageTagDesc) {
            LifeCycleConfigUserEntity info = new LifeCycleConfigUserEntity();
            try {
                BasicBSONList stageTagConfig = new BasicBSONList();
                BSONObject stageTag = new BasicBSONObject();
                stageTag.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME, stageTagName);
                stageTag.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC, stageTagDesc);
                stageTagConfig.add(stageTag);
                info.setStageTagConfig(stageTagConfig);
                info.setTransitionConfig(new BasicBSONList());
            }
            catch (Exception e) {
                logger.error(
                        "translate stage tag to LifeCycleConfigUserEntity failed:stageTagName={}",
                        stageTagName);
                throw e;
            }
            return info;
        }
    }

    public static class FullInfo {

        public static LifeCycleConfigFullEntity fromUserInfo(LifeCycleConfigUserEntity userInfo,
                String user, long createTime) {
            BasicBSONList transitionConfig = userInfo.getTransitionConfig();
            for (Object o : transitionConfig) {
                BSONObject transition = (BSONObject) o;
                transition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_WORKSPACES,
                        new BasicBSONList());
                transition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_ID,
                        ScmIdGenerator.ScheduleId.get(new Date()));
            }

            return new LifeCycleConfigFullEntity(userInfo.getStageTagConfig(), transitionConfig,
                    user, createTime, user, createTime);
        }

        public static LifeCycleConfigFullEntity fromBSONObject(BSONObject obj) {
            LifeCycleConfigFullEntity info = new LifeCycleConfigFullEntity();
            info.setCreateTime((Long) obj.get(FieldName.LifeCycleConfig.FIELD_CREATE_TIME));
            info.setCreateUser((String) obj.get(FieldName.LifeCycleConfig.FIELD_CREATE_USER));
            info.setStageTagConfig(
                    (BasicBSONList) obj.get(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_CONFIG));
            info.setTransitionConfig(
                    (BasicBSONList) obj.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_CONFIG));
            info.setUpdateTime((Long) obj.get(FieldName.LifeCycleConfig.FIELD_UPDATE_TIME));
            info.setUpdateUser((String) obj.get(FieldName.LifeCycleConfig.FIELD_UPDATE_USER));
            return info;
        }

        public static BSONObject toBSONObject(LifeCycleConfigFullEntity info) {
            BSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_CONFIG, info.getStageTagConfig());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CONFIG, info.getTransitionConfig());
            obj.put(FieldName.LifeCycleConfig.FIELD_CREATE_USER, info.getCreateUser());
            obj.put(FieldName.LifeCycleConfig.FIELD_CREATE_TIME, info.getCreateTime());
            obj.put(FieldName.LifeCycleConfig.FIELD_UPDATE_USER, info.getUpdateUser());
            obj.put(FieldName.LifeCycleConfig.FIELD_UPDATE_TIME, info.getUpdateTime());
            return obj;
        }

        public static LifeCycleConfigFullEntity updateFullInfoByAddStageTag(
                LifeCycleConfigFullEntity oldInfo, String stageTagName, String stageTagDesc,
                String updateUser) {
            BasicBSONList stageTagConfig = oldInfo.getStageTagConfig();
            BSONObject newStageTag = new BasicBSONObject();
            newStageTag.put(FieldName.LifeCycleConfig.FIELD_STAGE_NAME, stageTagName);
            newStageTag.put(FieldName.LifeCycleConfig.FIELD_STAGE_DESC, stageTagDesc);
            stageTagConfig.add(newStageTag);

            LifeCycleConfigFullEntity newInfo = new LifeCycleConfigFullEntity();
            newInfo.setCreateUser(oldInfo.getCreateUser());
            newInfo.setCreateTime(oldInfo.getCreateTime());
            newInfo.setTransitionConfig(oldInfo.getTransitionConfig());
            newInfo.setStageTagConfig(stageTagConfig);
            newInfo.setUpdateUser(updateUser);
            newInfo.setUpdateTime(new Date().getTime());
            return newInfo;
        }

        public static LifeCycleConfigFullEntity updateFullInfoByAddTransition(
                LifeCycleConfigFullEntity oldInfo, TransitionUserEntity transitionInfo,
                String updateUser) {
            BasicBSONList transitionConfig = oldInfo.getTransitionConfig();
            BSONObject newTransition = new BasicBSONObject();
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME,
                    transitionInfo.getName());
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW,
                    transitionInfo.getFlow().toBSONObj());
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER,
                    transitionInfo.getMatcher());
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT,
                    transitionInfo.getExtraContent().toBSONObj());
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS,
                    transitionInfo.getTransitionTriggers().toBSONObj());
            if (transitionInfo.getCleanTriggers() != null) {
                newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS,
                        transitionInfo.getCleanTriggers().toBSONObj());
            }
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_WORKSPACES,
                    new BasicBSONList());
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_ID,
                    ScmIdGenerator.ScheduleId.get(new Date()));
            transitionConfig.add(newTransition);

            LifeCycleConfigFullEntity newInfo = new LifeCycleConfigFullEntity();
            newInfo.setCreateUser(oldInfo.getCreateUser());
            newInfo.setCreateTime(oldInfo.getCreateTime());
            newInfo.setTransitionConfig(transitionConfig);
            newInfo.setStageTagConfig(oldInfo.getStageTagConfig());
            newInfo.setUpdateUser(updateUser);
            newInfo.setUpdateTime(new Date().getTime());

            return newInfo;
        }

        public static LifeCycleConfigFullEntity updateFullInfoByAlterTransition(
                LifeCycleConfigFullEntity oldInfo, TransitionUserEntity newTransitionInfo,
                TransitionFullEntity oldTransitionInfo, String updateUser) {
            BasicBSONList workspaces = oldTransitionInfo.getWorkspaces();
            String id = oldTransitionInfo.getId();
            LifeCycleConfigFullEntity info = updateFullInfoByRemoveTransition(
                    oldInfo, oldTransitionInfo.getName(), updateUser);
            BSONObject newTransition = TransitionEntityTranslator.UserInfo
                    .toBSONObject(newTransitionInfo);
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_ID, id);
            newTransition.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_WORKSPACES, workspaces);
            info.getTransitionConfig().add(newTransition);
            return info;
        }

        public static LifeCycleConfigFullEntity updateFullInfoByRemoveTransition(
                LifeCycleConfigFullEntity oldInfo, String transitionName, String updateUser) {
            BasicBSONList transitionConfig = oldInfo.getTransitionConfig();
            for (Object o : transitionConfig) {
                BSONObject transition = (BSONObject) o;
                if (transition.containsField(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME)
                        && transition.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME)
                                .equals(transitionName)) {
                    transitionConfig.remove(transition);
                    break;
                }
            }

            LifeCycleConfigFullEntity newInfo = new LifeCycleConfigFullEntity();
            newInfo.setCreateUser(oldInfo.getCreateUser());
            newInfo.setCreateTime(oldInfo.getCreateTime());
            newInfo.setTransitionConfig(transitionConfig);
            newInfo.setStageTagConfig(oldInfo.getStageTagConfig());
            newInfo.setUpdateTime(new Date().getTime());
            newInfo.setUpdateUser(updateUser);
            return newInfo;
        }

        public static LifeCycleConfigFullEntity updateFullInfoByRemoveStageTag(
                LifeCycleConfigFullEntity oldInfo, String stageTagName, String updateUser) {
            BasicBSONList stageTagConfig = oldInfo.getStageTagConfig();
            for (Object o : stageTagConfig) {
                BasicBSONObject stageTag = (BasicBSONObject) o;
                if (stageTag.containsField(FieldName.LifeCycleConfig.FIELD_STAGE_NAME)
                        && stageTag.getString(FieldName.LifeCycleConfig.FIELD_STAGE_NAME)
                                .equals(stageTagName)) {
                    stageTagConfig.remove(stageTag);
                    break;
                }
            }

            LifeCycleConfigFullEntity newInfo = new LifeCycleConfigFullEntity();
            newInfo.setCreateUser(oldInfo.getCreateUser());
            newInfo.setCreateTime(oldInfo.getCreateTime());
            newInfo.setTransitionConfig(oldInfo.getTransitionConfig());
            newInfo.setStageTagConfig(stageTagConfig);
            newInfo.setUpdateTime(new Date().getTime());
            newInfo.setUpdateUser(updateUser);
            return newInfo;
        }
    }
}
