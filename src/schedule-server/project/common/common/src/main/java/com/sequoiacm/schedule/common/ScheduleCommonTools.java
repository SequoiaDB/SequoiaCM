package com.sequoiacm.schedule.common;

import java.net.InetAddress;
import java.util.Date;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.common.model.ScheduleException;

public class ScheduleCommonTools {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleCommonTools.class);

    public static String getHostName() {
        String hostName = "";
        try {
            InetAddress ia = InetAddress.getLocalHost();
            hostName = ia.getHostName();
        }
        catch (Exception e) {
            logger.warn("get local hostname failed", e);
        }

        return hostName;
    }

    public static Object getValue(BSONObject obj, String key) {
        return obj.get(key);
    }

    public static Object getValueNotNull(BSONObject obj, String key) throws ScheduleException {
        Object v = null;
        try {
            v = getValue(obj, key);
            if (null == v) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.MISSING_ARGUMENT,
                        "key is no exist:obj=" + obj + ",key=" + key);
            }

            return v;
        }
        catch (ScheduleException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.MISSING_ARGUMENT,
                    "key is no exist:obj=" + obj + ",key=" + key, e);
        }
    }

    public static BSONObject getBSONObjectValue(BSONObject obj, String key)
            throws ScheduleException {
        Object v = getValue(obj, key);
        if (null == v) {
            return null;
        }

        try {
            BSONObject b = (BSONObject) v;
            return b;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "key's type is not BSONObject:key=" + key + ",value=" + v, e);
        }
    }

    public static boolean getBooleanOrElse(BSONObject obj, String key, boolean defaultValue)
            throws ScheduleException {
        Object v = getValue(obj, key);
        if (null == v) {
            return defaultValue;
        }
        try {
            return (boolean) v;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "key's type is not boolean:key=" + key + ",value=" + v, e);
        }
    }

    public static int getIntValue(BSONObject obj, String key) throws ScheduleException {
        Object v = getValueNotNull(obj, key);
        try {
            int i = (int) v;
            return i;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "key's type is not int:key=" + key + ",value=" + v, e);
        }
    }

    public static String getStringValue(BSONObject obj, String key) throws ScheduleException {
        Object v = getValueNotNull(obj, key);
        try {
            String s = (String) v;
            return s;
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "key's type is not String:key=" + key + ",value=" + v, e);
        }
    }

    public static BSONObject toBSONObject(String content) throws ScheduleException {
        BSONObject contentObj = null;
        try {
            contentObj = (BSONObject) JSON.parse(content);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "content is not a valid BSONObject:content=" + content);
        }

        return contentObj;
    }

    public static void exitProcess() {
        System.exit(-1);
    }

    public static String createContentServerUrl(String hostName, int port) {
        return hostName + ":" + port + "/api/v1/";
    }

    public static String createContentServerInternalUrl(String hostName, int port) {
        return hostName + ":" + port + "/internal/v1/";
    }

    public static String arrayToString(String[] urlElems) {
        if (null == urlElems) {
            return null;
        }

        return StringUtils.join(urlElems, ",");
    }

    private static String _joinUrlElems(String[] urlElems, String prefix) throws Exception {
        if (urlElems.length == 2) {
            return urlElems[0] + ":" + urlElems[1] + "/" + prefix + "/v1";
        }
        else if (urlElems.length == 3) {
            return urlElems[0] + ":" + urlElems[1] + "/" + prefix + "/" + urlElems[2];
        }
        else {
            throw new Exception("urlElems is invalid:urlElems=" + arrayToString(urlElems));
        }
    }

    public static String joinInternalUrlElems(String[] urlElems) throws Exception {
        return _joinUrlElems(urlElems, "internal");
    }

    public static String joinUrlElems(String[] urlElems) throws Exception {
        return _joinUrlElems(urlElems, "api");
    }

    public static BSONObject joinCreateTimeCondition(Date d, int existenceDays) {
        BSONObject ltCreateTimes = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_LT,
                d.getTime() - existenceDays * 24L * 3600L * 1000L);
        BSONObject fileCreateTimeLtTimes = new BasicBSONObject(
                FieldName.File.FIELD_CLFILE_FILE_CREATE_TIME, ltCreateTimes);
        return fileCreateTimeLtTimes;
    }

    public static BSONObject jointTriggerCondition(String scheduleType, BSONObject triggers,
            int sourceSiteId, int destSiteId, Date date) throws ScheduleException {
        BasicBSONList array = new BasicBSONList();
        BasicBSONList triggerList = BsonUtils.getArrayChecked(triggers,
                ScheduleDefine.TRIGGER_LIST);
        for (Object o : triggerList) {
            BSONObject trigger = (BSONObject) o;
            BSONObject condition = ScheduleCommonTools.transformTrigger(trigger, scheduleType,
                    sourceSiteId, destSiteId, date);
            array.add(condition);
        }
        String mode = BsonUtils.getStringChecked(triggers, ScheduleDefine.MODE);
        return new BasicBSONObject(ScheduleCommonTools.transformMode(mode), array);
    }

    public static BSONObject transformTrigger(BSONObject trigger, String type, int sourceSiteId,
            int destSiteId, Date date) throws ScheduleException {
        String mode = (String) trigger.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_MODE);
        if (type.equals(ScheduleDefine.ScheduleType.MOVE_FILE)
                || type.equals(ScheduleDefine.ScheduleType.COPY_FILE)) {
            // create_time
            BSONObject createTimeLtTimes = getCreateTimeCondition(trigger, date);

            // last_access_time
            BSONObject lastAccessTimeSiteCondition = getLastAccessTimeSiteCondition(date, trigger,
                    sourceSiteId);

            // build_access_time
            BSONObject buildTimeSiteCondition = getBuildTimeSiteCondition(date, trigger,
                    sourceSiteId);

            BasicBSONList array = new BasicBSONList();
            array.add(createTimeLtTimes);
            array.add(lastAccessTimeSiteCondition);
            array.add(buildTimeSiteCondition);

            return new BasicBSONObject(transformMode(mode), array);
        }
        else if (type.equals(ScheduleDefine.ScheduleType.CLEAN_FILE)) {
            // transitionTime
            BSONObject transitionTimeSiteCondition = getTransitionTimeSiteCondition(date, trigger,
                    destSiteId);

            BSONObject lastAccessTimeSiteCondition = getLastAccessTimeSiteCondition(date, trigger,
                    sourceSiteId);

            BasicBSONList array = new BasicBSONList();
            array.add(transitionTimeSiteCondition);
            array.add(lastAccessTimeSiteCondition);

            return new BasicBSONObject(transformMode(mode), array);
        }
        else {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    "invalid schedule type" + type);
        }
    }

    private static BSONObject getTransitionTimeSiteCondition(Date date, BSONObject trigger,
            int destSiteId) throws ScheduleException {
        return buildSiteCondition(date, FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME,
                (String) trigger
                        .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_TRANSITION_TIME),
                destSiteId);
    }

    private static BSONObject getBuildTimeSiteCondition(Date date, BSONObject trigger,
            int sourceSiteId) throws ScheduleException {
        return buildSiteCondition(date, FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME,
                (String) trigger.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_BUILD_TIME),
                sourceSiteId);
    }

    private static BSONObject getLastAccessTimeSiteCondition(Date date, BSONObject trigger,
            int sourceSiteId) throws ScheduleException {
        return buildSiteCondition(date,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_LAST_ACCESS_TIME,
                (String) trigger
                        .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_LAST_ACCESS_TIME),
                sourceSiteId);
    }

    private static BSONObject getCreateTimeCondition(BSONObject trigger, Date date)
            throws ScheduleException {
        int fileCreateTime = parseTime(
                FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_CREATE_TIME, (String) trigger
                        .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_CREATE_TIME));
        BSONObject fileCreateTimeLtTimes = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_LT,
                date.getTime() - fileCreateTime * 24L * 3600L * 1000L);
        return new BasicBSONObject(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME,
                fileCreateTimeLtTimes);
    }

    private static int parseTime(String timeName, String time) throws ScheduleException {
        String num = time.substring(0, time.length() - 1);
        if (num.isEmpty()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    timeName + " is invalid:" + timeName + "=" + time);
        }

        try {
            return Integer.parseInt(num);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    timeName + " is invalid:" + timeName + "=" + time);
        }
    }

    private static BSONObject buildSiteCondition(Date date, String timeName, String time,
            int siteId) throws ScheduleException {
        int days = parseTime(timeName, time);
        BSONObject lt = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_LT,
                date.getTime() - days * 24L * 3600L * 1000L);
        BSONObject ltTimes = new BasicBSONObject(timeName, lt);
        ltTimes.put(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_ID, siteId);

        BSONObject elemMatch = new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_ELEMMATCH,
                ltTimes);
        BSONObject siteCondition = new BasicBSONObject(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST,
                elemMatch);

        return siteCondition;
    }

    public static String transformMode(String mode) throws ScheduleException {
        if (mode.equals(ScheduleDefine.ModeType.ALL)) {
            return ScmQueryDefine.SEQUOIADB_MATCHER_AND;
        }
        else if (mode.equals(ScheduleDefine.ModeType.ANY)) {
            return ScmQueryDefine.SEQUOIADB_MATCHER_OR;
        }
        else {
            throw new ScheduleException(RestCommonDefine.ErrorCode.BAD_REQUEST,
                    "transition mode may choose all or any,mode=" + mode);
        }
    }

    public static int checkAndParseTime(String timeName, String time) throws ScheduleException {
        if (time.isEmpty()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    timeName + " can't be empty:" + timeName + "=" + time);
        }

        if (!time.endsWith("d")) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    timeName + " only supports day period(d):" + timeName + "=" + time);
        }

        String num = time.substring(0, time.length() - 1);
        if (num.isEmpty()) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    timeName + " is invalid:" + timeName + "=" + time);
        }

        try {
            return Integer.parseInt(num);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                    timeName + " is invalid:" + timeName + "=" + time, e);
        }
    }
}
