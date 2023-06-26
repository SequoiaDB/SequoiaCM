package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.RestCommonField;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public class ScheduleEntityTranslator {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleEntityTranslator.class);

    public static class Status {
        public static InternalSchStatus fromBSON(BSONObject bson) {
            InternalSchStatus ret = new InternalSchStatus();
            ret.setSchId(BsonUtils.getStringChecked(bson, FieldName.ScheduleStatus.FIELD_ID));
            ret.setSchName(BsonUtils.getString(bson, FieldName.ScheduleStatus.FIELD_NAME));
            ret.setStartTime(BsonUtils
                    .getNumberChecked(bson, FieldName.ScheduleStatus.FIELD_START_TIME).longValue());
            ret.setStatus(BsonUtils.getBSONChecked(bson, FieldName.ScheduleStatus.FIELD_STATUS));
            ret.setWorkerNode(
                    BsonUtils.getStringChecked(bson, FieldName.ScheduleStatus.FIELD_WORKER_NODE));
            ret.setFinish(BsonUtils.getBooleanOrElse(bson, FieldName.ScheduleStatus.FIELD_IS_FINISH,
                    false));
            return ret;
        }

        public static BSONObject toBSON(InternalSchStatus status) {
            BSONObject ret = new BasicBSONObject();
            ret.put(FieldName.ScheduleStatus.FIELD_ID, status.getSchId());
            ret.put(FieldName.ScheduleStatus.FIELD_NAME, status.getSchName());
            ret.put(FieldName.ScheduleStatus.FIELD_START_TIME, status.getStartTime());
            ret.put(FieldName.ScheduleStatus.FIELD_STATUS, status.getStatus());
            ret.put(FieldName.ScheduleStatus.FIELD_WORKER_NODE, status.getWorkerNode());
            ret.put(FieldName.ScheduleStatus.FIELD_IS_FINISH, status.isFinish());
            return ret;
        }
    }

    public static class FullInfo {
        public static ScheduleFullEntity fromUserInfo(ScheduleUserEntity userInfo, String id,
                String user, long createTime) {
            return new ScheduleFullEntity(id, userInfo.getName(), userInfo.getDesc(),
                    userInfo.getType(), userInfo.getWorkspace(), userInfo.getContent(),
                    userInfo.getCron(), userInfo.isEnable(), user, createTime,
                    userInfo.getPreferredRegion(), userInfo.getPreferredZone(),
                    userInfo.getTransitionId());
        }

        public static BSONObject toBSONObject(ScheduleFullEntity info) {
            BSONObject obj = new BasicBSONObject();
            obj.put(FieldName.Schedule.FIELD_ID, info.getId());
            obj.put(FieldName.Schedule.FIELD_NAME, info.getName());
            obj.put(FieldName.Schedule.FIELD_DESC, info.getDesc());
            obj.put(FieldName.Schedule.FIELD_TYPE, info.getType());
            obj.put(FieldName.Schedule.FIELD_WORKSPACE, info.getWorkspace());
            obj.put(FieldName.Schedule.FIELD_CONTENT, info.getContent());
            obj.put(FieldName.Schedule.FIELD_CRON, info.getCron());
            obj.put(FieldName.Schedule.FIELD_CREATE_USER, info.getCreate_user());
            obj.put(FieldName.Schedule.FIELD_CREATE_TIME, info.getCreate_time());
            obj.put(FieldName.Schedule.FIELD_ENABLE, info.isEnable());
            obj.put(FieldName.Schedule.FIELD_PREFERRED_REGION, info.getPreferredRegion());
            obj.put(FieldName.Schedule.FIELD_PREFERRED_ZONE, info.getPreferredZone());
            obj.put(FieldName.Schedule.FIELD_TRANSITION_ID, info.getTransitionId());
            return obj;
        }

        public static ScheduleFullEntity fromBSONObject(BSONObject obj) throws Exception {
            ScheduleFullEntity info = new ScheduleFullEntity();
            try {
                info.setId((String) obj.get(FieldName.Schedule.FIELD_ID));
                info.setName((String) obj.get(FieldName.Schedule.FIELD_NAME));
                info.setDesc((String) obj.get(FieldName.Schedule.FIELD_DESC));
                info.setType((String) obj.get(FieldName.Schedule.FIELD_TYPE));
                info.setWorkspace((String) obj.get(FieldName.Schedule.FIELD_WORKSPACE));
                info.setContent((BSONObject) obj.get(FieldName.Schedule.FIELD_CONTENT));
                info.setCron((String) obj.get(FieldName.Schedule.FIELD_CRON));
                info.setCreate_user((String) obj.get(FieldName.Schedule.FIELD_CREATE_USER));
                info.setCreate_time((long) obj.get(FieldName.Schedule.FIELD_CREATE_TIME));
                Object enable = obj.get(FieldName.Schedule.FIELD_ENABLE);
                if (enable != null) {
                    info.setEnable((boolean) enable);
                }
                info.setPreferredZone(
                        BsonUtils.getString(obj, FieldName.Schedule.FIELD_PREFERRED_ZONE));
                info.setPreferredRegion(
                        BsonUtils.getString(obj, FieldName.Schedule.FIELD_PREFERRED_REGION));
                Object transitionId = obj.get(FieldName.Schedule.FIELD_TRANSITION_ID);
                if (transitionId != null) {
                    info.setTransitionId((String) transitionId);
                }
            }
            catch (Exception e) {
                logger.error("translate BSONObject to ScheduleFullInfo failed:obj={}", obj);
                throw e;
            }

            return info;
        }
    }

    public static class NewUserInfo {
        public static ScheduleNewUserInfo fromJSON(String newInfoJSON) throws ScheduleException {
            ScheduleNewUserInfo info = new ScheduleNewUserInfo();
            BSONObject obj = ScheduleCommonTools.toBSONObject(newInfoJSON);
            info.setContent((BSONObject) obj.get(FieldName.Schedule.FIELD_CONTENT));
            info.setCron((String) obj.get(FieldName.Schedule.FIELD_CRON));
            info.setDesc((String) obj.get(FieldName.Schedule.FIELD_DESC));
            info.setName((String) obj.get(FieldName.Schedule.FIELD_NAME));
            info.setType((String) obj.get(FieldName.Schedule.FIELD_TYPE));
            info.setWorkspace((String) obj.get(FieldName.Schedule.FIELD_WORKSPACE));
            info.setEnable((Boolean) obj.get(FieldName.Schedule.FIELD_ENABLE));
            info.setPreferredRegion(BsonUtils.getString(obj, FieldName.Schedule.FIELD_PREFERRED_REGION));
            info.setPreferredZone(BsonUtils.getString(obj, FieldName.Schedule.FIELD_PREFERRED_ZONE));
            Object transitionId = obj.get(FieldName.Schedule.FIELD_TRANSITION_ID);
            if (transitionId != null) {
                info.setTransitionId((String) transitionId);
            }
            return info;
        }
    }

    public static class UserInfo {
        private static String getValueNotNull(HttpServletRequest request, String key)
                throws ScheduleException {
            String v = getValue(request, key);
            if (null == v) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.MISSING_ARGUMENT,
                        "miss argument:" + key);
            }

            return v;
        }

        private static String getValue(HttpServletRequest request, String key) {
            return request.getParameter(key);
        }

        public static ScheduleUserEntity fromBSONObject(BSONObject obj) {
            ScheduleUserEntity info = new ScheduleUserEntity();
            try {
                info.setName((String) obj.get(FieldName.Schedule.FIELD_NAME));
                info.setDesc((String) obj.get(FieldName.Schedule.FIELD_DESC));
                info.setType((String) obj.get(FieldName.Schedule.FIELD_TYPE));
                info.setWorkspace((String) obj.get(FieldName.Schedule.FIELD_WORKSPACE));
                info.setContent((BSONObject) obj.get(FieldName.Schedule.FIELD_CONTENT));
                info.setCron((String) obj.get(FieldName.Schedule.FIELD_CRON));

                Object enable = obj.get(FieldName.Schedule.FIELD_ENABLE);
                if (enable != null) {
                    info.setEnable((boolean) enable);
                }

                info.setPreferredRegion(
                        BsonUtils.getString(obj, FieldName.Schedule.FIELD_PREFERRED_REGION));
                info.setPreferredZone(
                        BsonUtils.getString(obj, FieldName.Schedule.FIELD_PREFERRED_ZONE));
                Object transitionId = obj.get(FieldName.Schedule.FIELD_TRANSITION_ID);
                if (transitionId != null) {
                    info.setTransitionId((String) transitionId);
                }
            }
            catch (Exception e) {
                logger.error("translate BSONObject to ScheduleUserEntity failed:obj={}", obj);
                throw e;
            }

            return info;
        }

        public static ScheduleUserEntity fromRequest(HttpServletRequest request)
                throws ScheduleException {
            String description = getValueNotNull(request,
                    RestCommonDefine.RestParam.KEY_DESCRIPTION);
            BSONObject obj = ScheduleCommonTools.toBSONObject(description);

            return fromBSONObject(obj);
        }

        public static String toJSONString(ScheduleUserEntity entity) throws ScheduleException {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.writeValueAsString(entity);
            }
            catch (JsonProcessingException e) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "formate ScheduleUserEntity failed:entity=" + entity, e);
            }
        }
    }
}
