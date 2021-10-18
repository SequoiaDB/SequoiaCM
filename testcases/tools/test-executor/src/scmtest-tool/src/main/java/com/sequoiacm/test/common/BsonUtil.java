package com.sequoiacm.test.common;

import org.bson.BSONObject;

public class BsonUtil {

    private BsonUtil() {
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

    public static String getString(BSONObject object, String field) {
        return get(object, field);
    }

    public static String getStringChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static Integer getIntegerChecked(BSONObject object, String field) {
        Object obj = getChecked(object, field);
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        return Integer.valueOf((String) obj);
    }

    public static Boolean getBoolean(BSONObject object, String field) {
        return get(object, field);
    }

    public static Boolean getBooleanChecked(BSONObject object, String field) {
        return Boolean.valueOf(getChecked(object, field));
    }
}
