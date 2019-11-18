package com.sequoiacm.deploy.common;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

public final class BsonUtils {
    private BsonUtils() {
    }

    @SuppressWarnings("unchecked")
    private static <F> F get(BSONObject object, String field) {
        return (F) object.get(field);
    }

    private static <F> F getChecked(BSONObject object, String field) {
        F f = get(object, field);
        if (f == null) {
            throw new IllegalArgumentException("missing field " + field);
        }
        return f;
    }

    private static <F> F getOrElse(BSONObject object, String field, F defaultValue) {
        F f = get(object, field);
        if (f == null) {
            f = defaultValue;
        }
        return f;
    }

    public static Object getObject(BSONObject object, String field) {
        return get(object, field);
    }

    public static Object getObjectChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static Object getObjectOrElse(BSONObject object, String field, Object defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public static String getString(BSONObject object, String field) {
        return get(object, field);
    }

    public static String getStringChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static String getStringOrElse(BSONObject object, String field, String defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public static Number getNumber(BSONObject object, String field) {
        return get(object, field);
    }

    public static Number getNumberChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static Number getNumberOrElse(BSONObject object, String field, Number defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public static Long getLong(BSONObject object, String field) {
        return get(object, field);
    }

    public static Long getLongChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static Long getLongOrElse(BSONObject object, String field, long defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public Integer getInteger(BSONObject object, String field) {
        return get(object, field);
    }

    public static Integer getIntegerChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static Integer getIntegerOrElse(BSONObject object, String field, int defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public static Boolean getBoolean(BSONObject object, String field) {
        return get(object, field);
    }

    public static Boolean getBooleanChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static Boolean getBooleanOrElse(BSONObject object, String field, boolean defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public static BasicBSONList getArray(BSONObject object, String field) {
        return get(object, field);
    }

    public static BasicBSONList getArrayChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static BasicBSONList getArrayOrElse(BSONObject object, String field,
            BasicBSONList defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public static BSONObject getBSONObjectChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static BSONObject getBSONObject(BSONObject object, String field) {
        return get(object, field);
    }

    public static Date getDateChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

}
