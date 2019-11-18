package com.sequoiacm.schedule.common;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.schedule.exception.ScheduleException;


public class ScmArgChecker {
    public static class ExtralCondition {
        private static final List<String> HISTORY_MATCHER_VALID_KEY = new ArrayList<>();
        static {
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_DATA_ID);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_ID);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_DATA_TYPE);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_MAJOR_VERSION);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_MINOR_VERSION);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_SIZE);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_ID);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_SITE_LIST_TIME);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_INNER_CREATE_MONTH);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.File.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
        }



        public static void checkHistoryFileMatcher(BSONObject fileMatcher)
                throws ScheduleException {
            if (fileMatcher == null) {
                return;
            }
            if (fileMatcher instanceof BasicBSONList) {
                BasicBSONList list = (BasicBSONList) fileMatcher;
                for (Object ele : list) {
                    if (ele instanceof BSONObject) {
                        checkHistoryFileMatcher((BSONObject) ele);
                    }
                }
            }
            else {
                for (String key : fileMatcher.keySet()) {
                    if (!HISTORY_MATCHER_VALID_KEY.contains(key) && !key.startsWith("$")) {
                        throw new ScheduleException(RestCommonDefine.ErrorCode.INVALID_ARGUMENT,
                                "query in history table, matcher contains invalid key:key=" + key);
                    }
                    Object value = fileMatcher.get(key);
                    if (value instanceof BSONObject) {
                        checkHistoryFileMatcher((BSONObject) value);
                    }
                }
            }
        }
    }
}
