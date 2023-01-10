package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.infrastructure.common.BsonUtils;
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

public class TransitionEntityTranslator {
    private static final Logger logger = LoggerFactory.getLogger(TransitionEntityTranslator.class);

    public static class UserInfo {
        public static TransitionUserEntity fromBSONObject(BSONObject obj) {
            TransitionUserEntity info = new TransitionUserEntity();
            try {
                info.setName(
                        BsonUtils.getString(obj, FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME));

                BSONObject flow = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW);
                info.setFlow(new ScmFlow(flow));

                BSONObject extraContent = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT);
                info.setExtraContent(new ScmExtraContent(extraContent));

                BSONObject transitionTriggers = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS);
                info.setTransitionTriggers(new ScmTransitionTriggers(transitionTriggers));

                BSONObject cleanTriggers = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS);
                if (cleanTriggers != null) {
                    info.setCleanTriggers(new ScmCleanTriggers(cleanTriggers));
                }
                info.setMatcher(
                        BsonUtils.getBSON(obj, FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER));
                return info;
            }
            catch (Exception e) {
                logger.error("translate BSONObject to TransitionUserEntity failed:obj={}",
                        obj);
                throw e;
            }
        }

        public static TransitionUserEntity analyzeConfig(String transition)
                throws ScheduleException {
            if (!StringUtils.hasText(transition)) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.MISSING_ARGUMENT,
                        "miss argument:"
                                + RestCommonField.TRANSITION);
            }
            BSONObject info = LifeCycleCommonTools.toBSONObject(
                    RestCommonField.TRANSITION, transition);
            return fromBSONObject(info);
        }

        public static String toJSONString(TransitionUserEntity entity) throws ScheduleException {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.writeValueAsString(entity);
            }
            catch (JsonProcessingException e) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "formate TransitionUserEntity failed:entity=" + entity, e);
            }
        }

        public static BSONObject toBSONObject(TransitionUserEntity info) {
            BSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME, info.getName());

            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW, info.getFlow().toBSONObj());

            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS,
                    info.getTransitionTriggers().toBSONObj());

            if (info.getCleanTriggers() != null) {
                obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS,
                        info.getCleanTriggers().toBSONObj());
            }

            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER, info.getMatcher());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT,
                    info.getExtraContent().toBSONObj());
            return obj;
        }
    }

    public static class FullInfo {
        public static TransitionFullEntity fromBSONObject(BSONObject obj) {
            TransitionFullEntity info = new TransitionFullEntity();
            try {
                info.setName(
                        BsonUtils.getString(obj, FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME));

                BSONObject flow = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW);
                info.setFlow(new ScmFlow(flow));

                BSONObject extraContent = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT);
                info.setExtraContent(new ScmExtraContent(extraContent));

                BSONObject transitionTriggers = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS);
                info.setTransitionTriggers(new ScmTransitionTriggers(transitionTriggers));

                BSONObject cleanTriggers = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS);
                if (cleanTriggers != null) {
                    info.setCleanTriggers(new ScmCleanTriggers(cleanTriggers));
                }

                info.setMatcher(
                        (BSONObject) obj.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER));
                info.setWorkspaces((BasicBSONList) obj
                        .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_WORKSPACES));
                info.setId((String) obj.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_ID));
                return info;
            }
            catch (Exception e) {
                logger.error("translate BSONObject to LifeCycleConfigUserEntity failed:obj={}",
                        obj);
                throw e;
            }
        }

        public static BSONObject toBSONObject(TransitionFullEntity info) {
            BSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME, info.getName());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW, info.getFlow().toBSONObj());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS,
                    info.getTransitionTriggers().toBSONObj());

            if (info.getCleanTriggers() != null) {
                obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS,
                        info.getCleanTriggers().toBSONObj());
            }

            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER, info.getMatcher());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT,
                    info.getExtraContent().toBSONObj());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_WORKSPACES, info.getWorkspaces());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_ID, info.getId());
            return obj;
        }
    }

    public static class WsFullInfo {
        public static TransitionScheduleEntity fromBSONObject(BSONObject obj) {
            TransitionScheduleEntity info = new TransitionScheduleEntity();

            try {
                info.setId(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID));
                info.setWorkspace(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME));

                TransitionUserEntity transition = new TransitionUserEntity();
                transition.setName(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME));
                transition.setMatcher(
                        BsonUtils.getBSON(obj, FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER));

                BSONObject extraContent = BsonUtils.getBSONChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT);
                transition.setExtraContent(new ScmExtraContent(extraContent));

                BSONObject transitionTriggers = BsonUtils.getBSONChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS);
                transition.setTransitionTriggers(new ScmTransitionTriggers(transitionTriggers));

                BSONObject cleanTriggers = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS);
                if (cleanTriggers != null) {
                    transition.setCleanTriggers(new ScmCleanTriggers(cleanTriggers));
                }

                BSONObject flow = BsonUtils.getBSON(obj,
                        FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW);
                transition.setFlow(new ScmFlow(flow));

                info.setTransition(transition);

                info.setCustomized(BsonUtils.getBooleanChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_HADCUSTOM));
                info.setScheduleIds(BsonUtils.getArrayChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_IDS));
                info.setCreateTime(BsonUtils.getLongChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_TIME));
                info.setCreateUser(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_USER));
                info.setEnable(BsonUtils.getBooleanChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ENABLE));
                info.setPreferredRegion(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_PREFERRED_REGION));
                info.setPreferredZone(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_PREFERRED_ZONE));
                info.setUpdateUser(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_USER));
                info.setUpdateTime(BsonUtils.getLongChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_TIME));
                info.setGlobalTransitionId(
                        BsonUtils.getString(obj, FieldName.LifeCycleConfig.FIELD_WS_TRANSITION_ID));
                return info;
            }
            catch (Exception e) {
                logger.error(
                        "life cycle config schedule BSONObject to TransitionScheduleEntity failed:obj={}",
                        obj);
                throw e;
            }
        }

        public static BSONObject toBSONObject(TransitionScheduleEntity info) {
            BSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID, info.getId());
            obj.put(FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME, info.getWorkspace());
            obj.put(FieldName.LifeCycleConfig.FIELD_WS_TRANSITION_ID, info.getGlobalTransitionId());

            TransitionUserEntity transition = info.getTransition();
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME, transition.getName());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW,
                    transition.getFlow().toBSONObj());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER, transition.getMatcher());

            if (transition.getCleanTriggers() != null) {
                obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS,
                        transition.getCleanTriggers().toBSONObj());
            }

            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS,
                    transition.getTransitionTriggers().toBSONObj());
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT,
                    transition.getExtraContent().toBSONObj());

            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_HADCUSTOM,
                    info.getCustomized());
            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ENABLE,
                    info.isEnable());
            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_USER,
                    info.getCreateUser());
            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_TIME,
                    info.getCreateTime());
            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_IDS,
                    info.getScheduleIds());
            obj.put(FieldName.LifeCycleConfig.FIELD_PREFERRED_REGION, info.getPreferredRegion());
            obj.put(FieldName.LifeCycleConfig.FIELD_PREFERRED_ZONE, info.getPreferredZone());
            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_USER,
                    info.getUpdateUser());
            obj.put(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_TIME,
                    info.getUpdateTime());
            return obj;
        }

        public static TransitionScheduleEntity fromDecoder(BSONObject obj) {
            TransitionScheduleEntity info = new TransitionScheduleEntity();

            try {
                info.setId(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID));
                info.setWorkspace(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME));
                info.setTransition(UserInfo.fromBSONObject(BsonUtils.getBSONChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_TRANSITION)));

                info.setCustomized(BsonUtils.getBooleanChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_HADCUSTOM));
                info.setScheduleIds(BsonUtils.getArrayChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_IDS));
                info.setCreateTime(BsonUtils.getLongChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_TIME));
                info.setCreateUser(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_USER));
                info.setEnable(BsonUtils.getBooleanChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ENABLE));
                info.setPreferredRegion(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_PREFERRED_REGION));
                info.setPreferredZone(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_PREFERRED_ZONE));
                info.setUpdateUser(BsonUtils.getStringChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_USER));
                info.setUpdateTime(BsonUtils.getLongChecked(obj,
                        FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_TIME));
                info.setGlobalTransitionId(
                        BsonUtils.getString(obj, FieldName.LifeCycleConfig.FIELD_WS_TRANSITION_ID));
                return info;
            }
            catch (Exception e) {
                logger.error(
                        "transition BSONObject to TransitionScheduleEntity failed:obj={}",
                        obj);
                throw e;
            }
        }

        public static TransitionScheduleEntity fromTransitionInfo(TransitionUserEntity info,
                String id, String workspace, String user, long createTime, boolean status,
                boolean hadCustom, String preferredRegion, String preferredZone, String updateUser,
                long updateTime) {
            return new TransitionScheduleEntity(id, workspace, info, hadCustom, createTime, user,
                    status, preferredRegion, preferredZone, updateUser, updateTime);
        }
    }
}
