package com.sequoiacm.schedule.common;

import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
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
}
