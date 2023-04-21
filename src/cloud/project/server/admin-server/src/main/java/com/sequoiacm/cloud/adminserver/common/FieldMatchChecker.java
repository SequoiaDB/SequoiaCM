package com.sequoiacm.cloud.adminserver.common;

import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldMatchChecker {

    public enum CheckType {
        OBJECT_DELTA
    }

    private static final List<String> OBJECT_DELTA = new ArrayList<String>();
    private static final Map<CheckType, List<String>> VALID_KEY_MAP = new HashMap<>();
    static {
        OBJECT_DELTA.add(FieldName.ObjectDelta.FIELD_SIZE_DELTA);
        OBJECT_DELTA.add(FieldName.ObjectDelta.FIELD_BUCKET_NAME);
        OBJECT_DELTA.add(FieldName.ObjectDelta.FIELD_COUNT_DELTA);
        OBJECT_DELTA.add(FieldName.ObjectDelta.FIELD_RECORD_TIME);
        OBJECT_DELTA.add(FieldName.ObjectDelta.FIELD_UPDATE_TIME);

        VALID_KEY_MAP.put(CheckType.OBJECT_DELTA, OBJECT_DELTA);
    }

    public static void checkFields(BSONObject fields, CheckType checkType)
            throws StatisticsException {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        if (fields instanceof BasicBSONList) {
            BasicBSONList list = (BasicBSONList) fields;
            for (Object ele : list) {
                if (ele instanceof BSONObject) {
                    checkFields((BSONObject) ele, checkType);
                }
            }
        }
        else {
            for (String key : fields.keySet()) {
                List<String> validKeys = VALID_KEY_MAP.get(checkType);
                if (!validKeys.contains(key) && !key.startsWith("$")) {
                    throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                            "invalid matcher key:key=" + key);
                }
                Object value = fields.get(key);
                if (value instanceof BSONObject) {
                    checkFields((BSONObject) value, checkType);
                }
            }
        }
    }

}
