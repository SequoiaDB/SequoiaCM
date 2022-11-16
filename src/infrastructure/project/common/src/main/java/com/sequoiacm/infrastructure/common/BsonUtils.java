package com.sequoiacm.infrastructure.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.Map;

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
            throw new IllegalArgumentException("missing field:field= " + field + ", obj=" + object);
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

    public static Integer getInteger(BSONObject object, String field) {
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

    public static BSONObject getBSON(BSONObject object, String field) {
        return get(object, field);
    }

    public static BSONObject getBSONOrElse(BSONObject object, String field, BSONObject defaultValue) {
        return getOrElse(object, field, defaultValue);
    }

    public static BSONObject getBSONChecked(BSONObject object, String field) {
        return getChecked(object, field);
    }

    public static BSONObject deepCopyRecordBSON(BSONObject obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj instanceof BasicBSONObject) {
                return deepCopyBasicBSON((BasicBSONObject) obj);
            }
            if (obj instanceof BasicBSONList) {
                return deepCopyBasicBSONList((BasicBSONList) obj);
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException(
                    "deep copy failed:" + obj.getClass().getName() + ", " + obj.toString(), e);
        }
        throw new IllegalArgumentException("deep copy failed: unknown type:"
                + obj.getClass().getName() + ", " + obj.toString());
    }

    private static BasicBSONObject deepCopyBasicBSON(BasicBSONObject bson) {
        if (bson == null) {
            return null;
        }
        BasicBSONObject ret = new BasicBSONObject();
        for (Map.Entry<String, Object> e : bson.entrySet()) {
            ret.put(e.getKey(), deepCopyRecordObject(e.getValue()));
        }
        return ret;
    }

    public static BasicBSONList deepCopyBasicBSONList(BasicBSONList bson) {
        if (bson == null) {
            return null;
        }
        BasicBSONList ret = new BasicBSONList();
        for (Object e : bson) {
            ret.add(deepCopyRecordObject(e));
        }
        return ret;
    }

    private static Object deepCopyRecordObject(Object obj) {
        if (noNeedCopy(obj)) {
            // 基本类型(数值、布尔）、字符串、null、无需拷贝
            return obj;
        }
        if (obj instanceof BasicBSONObject) {
            return deepCopyBasicBSON((BasicBSONObject) obj);
        }
        if (obj instanceof BasicBSONList) {
            return deepCopyBasicBSONList((BasicBSONList) obj);
        }
        throw new IllegalArgumentException("deep copy failed: unknown type:"
                + obj.getClass().getName() + ", " + obj.toString());
    }

    private static boolean noNeedCopy(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof Boolean) {
            return true;
        }
        if (o instanceof Character) {
            return true;
        }
        if (o instanceof Byte) {
            return true;
        }
        if (o instanceof Short) {
            return true;
        }
        if (o instanceof Integer) {
            return true;
        }
        if (o instanceof Long) {
            return true;
        }
        if (o instanceof Float) {
            return true;
        }
        if (o instanceof Double) {
            return true;
        }
        if (o instanceof String) {
            return true;
        }
        return false;
    }
}
