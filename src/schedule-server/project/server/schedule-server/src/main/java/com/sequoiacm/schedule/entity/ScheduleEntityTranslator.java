package com.sequoiacm.schedule.entity;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.ScheduleCommonTools;
import com.sequoiacm.schedule.exception.ScheduleException;

public class ScheduleEntityTranslator {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleEntityTranslator.class);

    public static class FullInfo {
        public static ScheduleFullEntity fromUserInfo(ScheduleUserEntity userInfo, String id,
                String user, long createTime) {
            ScheduleFullEntity fullInfo = new ScheduleFullEntity(id, userInfo.getName(),
                    userInfo.getDesc(), userInfo.getType(), userInfo.getWorkspace(),
                    userInfo.getContent(), userInfo.getCron(), userInfo.isEnable(), user,
                    createTime);

            return fullInfo;
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

            BSONObject obj = ScheduleCommonTools.toBSONObjct(newInfoJSON);

            info.setContent((BSONObject) obj.get(FieldName.Schedule.FIELD_CONTENT));
            info.setCron((String) obj.get(FieldName.Schedule.FIELD_CRON));
            info.setDesc((String) obj.get(FieldName.Schedule.FIELD_DESC));
            info.setName((String) obj.get(FieldName.Schedule.FIELD_NAME));
            info.setType((String) obj.get(FieldName.Schedule.FIELD_TYPE));
            info.setWorkspace((String) obj.get(FieldName.Schedule.FIELD_WORKSPACE));
            info.setEnable((Boolean) obj.get(FieldName.Schedule.FIELD_ENABLE));
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
            BSONObject obj = ScheduleCommonTools.toBSONObjct(description);

            return fromBSONObject(obj);
        }

        public static NameValuePair toNameValuePair(HttpServletRequest request)
                throws ScheduleException {
            String description = getValueNotNull(request,
                    RestCommonDefine.RestParam.KEY_DESCRIPTION);
            return new BasicNameValuePair(RestCommonDefine.RestParam.KEY_DESCRIPTION, description);
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
