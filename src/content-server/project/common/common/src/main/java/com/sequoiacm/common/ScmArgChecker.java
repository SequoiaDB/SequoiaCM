package com.sequoiacm.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmArgChecker {
    private static final Logger logger = LoggerFactory.getLogger(ScmArgChecker.class);

    public static class File {
        enum CheckType {
            BUCKET_FILE,
            HISTORY_FILE
        }

        private static final List<String> HISTORY_MATCHER_VALID_KEY = new ArrayList<String>();
        private static final List<String> BUCKET_FILE_MATCHER_VALID_KEY = new ArrayList<String>();
        private static final Map<CheckType, List<String>> VALID_KEY_MAP = new HashMap();
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
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_TAGS);
            HISTORY_MATCHER_VALID_KEY.add(FieldName.FIELD_CLFILE_CUSTOM_TAG);

            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_ID);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_NAME);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_ETAG);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_MAJOR_VERSION);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_MINOR_VERSION);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_UPDATE_TIME);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_CREATE_USER);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_MIME_TYPE);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_SIZE);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_CREATE_TIME);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_VERSION_SERIAL);
            BUCKET_FILE_MATCHER_VALID_KEY.add(FieldName.BucketFile.FILE_DELETE_MARKER);

            VALID_KEY_MAP.put(CheckType.HISTORY_FILE, HISTORY_MATCHER_VALID_KEY);
            VALID_KEY_MAP.put(CheckType.BUCKET_FILE, BUCKET_FILE_MATCHER_VALID_KEY);
        }

        public static boolean checkFileName(String name) {
            if (name == null) {
                return false;
            }
            if (name.length() <= 0) {
                return false;
            }
            return true;
        }

        public static void checkFileFields(BSONObject fields, CheckType checkType)
                throws InvalidArgumentException {
            if (fields == null) {
                return;
            }
            if (fields instanceof BasicBSONList) {
                BasicBSONList list = (BasicBSONList) fields;
                for (Object ele : list) {
                    if (ele instanceof BSONObject) {
                        checkFileFields((BSONObject) ele, checkType);
                    }
                }
            }
            else {
                for (String key : fields.keySet()) {
                    List<String> validKeys = VALID_KEY_MAP.get(checkType);
                    if (!validKeys.contains(key) && !key.startsWith("$")) {
                        if (CheckType.HISTORY_FILE == checkType) {
                            if (isExternalData(key)) {
                                continue;
                            }
                        }
                        if (isSiteListDollerNum(key)) {
                            continue;
                        }
                        throw new InvalidArgumentException("invalid key:key=" + key);
                    }
                    Object value = fields.get(key);
                    if (value instanceof BSONObject) {
                        checkFileFields((BSONObject) value, checkType);
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
                if (arr[2].equals(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION)) {
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

        public static void checkHistoryFileOrderby(BSONObject orderby)
                throws InvalidArgumentException {
            try {
                checkFileFields(orderby, CheckType.HISTORY_FILE);
            }
            catch (InvalidArgumentException e) {
                throw new InvalidArgumentException(
                        "the orderby parameter contains an invalid key: " + e.getMessage(), e);
            }
        }

        public static void checkHistoryFileMatcher(BSONObject fileMatcher)
                throws InvalidArgumentException {
            try {
                checkFileFields(fileMatcher, CheckType.HISTORY_FILE);
            }
            catch (InvalidArgumentException e) {
                throw new InvalidArgumentException(
                        "the matcher parameter contains an invalid key: " + e.getMessage(), e);
            }
        }

        public static void checkBucketFileOrderBy(BSONObject orderby)
                throws InvalidArgumentException {
            try {
                checkFileFields(orderby, CheckType.BUCKET_FILE);
            }
            catch (InvalidArgumentException e) {
                throw new InvalidArgumentException(
                        "the orderby parameter contains an invalid key: " + e.getMessage(), e);
            }
        }

        public static void checkBucketFileMatcher(BSONObject fileMatcher)
                throws InvalidArgumentException {
            try {
                checkFileFields(fileMatcher, CheckType.BUCKET_FILE);
            }
            catch (InvalidArgumentException e) {
                throw new InvalidArgumentException(
                        "the matcher parameter contains an invalid key: " + e.getMessage(), e);
            }
        }

        public static void checkScmCustomTag(Map<String, String> customTag)
                throws ScmServerException {
            if (customTag.size() > 10) {
                throw new ScmServerException(ScmError.FILE_CUSTOMTAG_TOO_LARGE,
                        "the file tag number can not more than 10");
            }

            for (Map.Entry<String, String> entry : customTag.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null || key.length() == 0) {
                    throw new ScmServerException(ScmError.FILE_INVALID_CUSTOMTAG,
                            "the file tag key is null or empty");
                }
                if (key.length() > 128) {
                    throw new ScmServerException(ScmError.FILE_INVALID_CUSTOMTAG,
                            "the file tag key length can not more than 128");
                }
                if (value != null && value.length() > 256) {
                    throw new ScmServerException(ScmError.FILE_INVALID_CUSTOMTAG,
                            "the file tag value length more than 256");
                }
            }
        }

        public static void checkAndCorrectS3Tag(Map<String, String> s3Tag)
                throws ScmServerException {
            if (s3Tag.size() > 10) {
                throw new ScmServerException(ScmError.FILE_CUSTOMTAG_TOO_LARGE,
                        "the file tag number can not more than 10");
            }

            Iterator<Map.Entry<String, String>> it = s3Tag.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                if (entry.getValue() == null) {
                    it.remove();
                }
            }
            checkScmCustomTag(s3Tag);
        }
    }

    public static class Bucket {
        public static boolean checkBucketName(String name) {
            if (name.length() < 3 || name.length() > 63) {
                return false;
            }
            if (!name.matches("^[a-z0-9][a-z0-9._-]+[a-z0-9]$")) {
                return false;
            }
            return true;
        }

        public static void checkBucketTag(Map<String, String> customTag) throws ScmServerException {
            if (customTag.size() > 50) {
                throw new ScmServerException(ScmError.BUCKET_CUSTOMTAG_TOO_LARGE,
                        "the bucket tag number can not more than 50");
            }

            for (String key : customTag.keySet()) {
                if (key == null || key.length() == 0) {
                    throw new ScmServerException(ScmError.BUCKET_INVALID_CUSTOMTAG,
                            "the bucket tag key is null or empty");
                }
            }
        }
    }

    public static class Directory {
        public static boolean checkDirectoryName(String name) {
            if (name == null) {
                return false;
            }
            if (name.length() <= 0) {
                return false;
            }

            if (name.contains("/") || name.contains("\\") || name.contains("%")
                    || name.contains(";") || name.contains(":") || name.contains("*")
                    || name.contains("?") || name.contains("\"") || name.contains("<")
                    || name.contains(">") || name.contains("|")) {
                return false;
            }

            if (name.equals(".")) {
                return false;
            }
            return true;
        }
    }

    public static class Workspace {
        public static boolean checkWorkspaceName(String name) {
            if (name == null) {
                return false;
            }
            // if name exist char of the Chinese,char size != Byte size
            if (name.length() <= 0 || name.length() != name.getBytes().length) {
                return false;
            }

            for (int index = 0; index < name.length(); index++) {
                char ch = name.charAt(index);
                if (ch == '/' || ch == '\\' || ch == '%' || ch == ';' || ch == '|' || ch == '.'
                        || ch == ':' || ch == '*' || ch == '?' || ch == '<' || ch == '>'
                        || ch == '"') {
                    return false;
                }
            }
            return true;
        }

    }

    public static boolean checkUriPathArg(String uriPathArg) {
        if (uriPathArg == null || uriPathArg.length() <= 0) {
            return false;
        }

        for (int index = 0; index < uriPathArg.length(); index++) {
            char ch = uriPathArg.charAt(index);
            if (ch == '/' || ch == '\\' || ch == '%' || ch == ';') {
                return false;
            }
        }
        return true;
    }
}
