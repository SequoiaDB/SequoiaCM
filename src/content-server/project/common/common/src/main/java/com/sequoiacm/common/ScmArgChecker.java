package com.sequoiacm.common;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmArgChecker {
    private static final Logger logger = LoggerFactory.getLogger(ScmArgChecker.class);

    public static class File {
        private static final List<String> HISTORY_MATCHER_VALID_KEY = new ArrayList<String>();
        static {
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_DATA_ID);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_ID);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_DATA_TYPE);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_MAJOR_VERSION);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_MINOR_VERSION);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_SIZE);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        }

        public static boolean checkFileName(String name) {
            if (name == null) {
                return false;
            }
            if (name.length() <= 0) {
                return false;
            }

            if (name.contains("/") || name.contains("\\") || name.contains(":")
                    || name.contains("*") || name.contains("?") || name.contains("\"")
                    || name.contains("<") || name.contains(">") || name.contains("|")) {
                return false;
            }

            if (name.equals(".")) {
                return false;
            }

            return true;
        }

        public static void checkHistoryFileMatcher(BSONObject fileMatcher)
                throws InvalidArgumentException {
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
                        if (isExternalData(key)) {
                            continue;
                        }
                        if (isSiteListDollerNum(key)) {
                            continue;
                        }
                        throw new InvalidArgumentException(
                                "query in history table, matcher contains invalid key:key=" + key);
                    }
                    Object value = fileMatcher.get(key);
                    if (value instanceof BSONObject) {
                        checkHistoryFileMatcher((BSONObject) value);
                    }
                }
            }
        }

        private static boolean isExternalData(String key) {
            if (key.startsWith(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + ".")) {
                return true;
            }
            return false;
        }

        private static boolean isSiteListDollerNum(String key) {
            if (key.endsWith(".")) {
                return false;
            }
            String[] arr = key.split("\\.");
            if (arr.length < 2 || arr.length > 3) {
                return false;
            }
            if (!arr[0].equals(FieldName.FIELD_CLFILE_FILE_SITE_LIST)) {
                return false;
            }
            String index;
            if (arr[1].startsWith("$")) {
                index = arr[1].substring(1);
            }
            else {
                index = arr[1];
            }
            try {
                if (Integer.valueOf(index) < 0) {
                    return false;
                }
            }
            catch (Exception e) {
                return false;
            }

            if (arr.length == 3) {
                if (arr[2].equals(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID)) {
                    return true;
                }
                if (arr[2].equals(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME)) {
                    return true;
                }
                if (arr[2].equals(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME)) {
                    return true;
                }
                return false;
            }
            return true;
        }

        public static boolean isHistoryFileBsonContainInvlidKey(BSONObject historyFileBson) {
            try {
                checkHistoryFileMatcher(historyFileBson);
                return true;
            }
            catch (InvalidArgumentException e) {
                logger.debug("HistoryFileBson contain invlid key", e);
                return false;
            }
        }
    }

    public static class Directory {
        public static boolean checkDirectoryName(String name) {
            return File.checkFileName(name);
        }
    }

    public static class Workspace {
        public static boolean checkWorkspaceName(String name) {
            if (name == null) {
                return false;
            }
            if (name.length() <= 0) {
                return false;
            }

            if (name.contains(":")) {
                return false;
            }
            return true;
        }
    }

}
