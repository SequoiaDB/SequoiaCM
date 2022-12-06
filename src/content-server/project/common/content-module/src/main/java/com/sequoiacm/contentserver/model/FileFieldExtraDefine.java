package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileFieldExtraDefine {
    // 用户能自由指定的文件属性
    public static Set<String> USER_FIELD = new HashSet<>();
    static {
        USER_FIELD.add(FieldName.FIELD_CLFILE_NAME);
        USER_FIELD.add(FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        USER_FIELD.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        USER_FIELD.add(FieldName.FIELD_CLFILE_BATCH_ID);
        USER_FIELD.add(FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        USER_FIELD.add(FieldName.FIELD_CLFILE_PROPERTIES);
        USER_FIELD.add(FieldName.FIELD_CLFILE_TAGS);
        USER_FIELD.add(FieldName.FIELD_CLFILE_FILE_TITLE);
        USER_FIELD.add(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        USER_FIELD.add(FieldName.FIELD_CLREL_FILE_MIME_TYPE);
        USER_FIELD.add(FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        USER_FIELD.add(FieldName.FIELD_CLFILE_CUSTOM_TAG);
    }

    public static boolean isUserField(String key) {
        if (USER_FIELD.contains(key)) {
            return true;
        }

        if (key.startsWith(FieldName.FIELD_CLFILE_PROPERTIES + ".")) {
            return true;
        }
        return false;
    }

    public static final Set<String> AVAILABLE_FIELD = new HashSet<>();
    static {
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_NAME);
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_FILE_TITLE);
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);

        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        AVAILABLE_FIELD.add(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);

        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_PROPERTIES);
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_TAGS);
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        AVAILABLE_FIELD.add(FieldName.FIELD_CLFILE_CUSTOM_TAG);
    }

    public static void checkAvailableFields(Set<String> objFields) throws ScmServerException {
        for (String field : objFields) {
            // SEQUOIACM-312
            // {class_properties.key:value}
            if (field.startsWith(FieldName.FIELD_CLFILE_PROPERTIES + ".")) {
                String subKey = field.substring((FieldName.FIELD_CLFILE_PROPERTIES + ".").length());
                MetaDataManager.getInstence().validateKeyFormat(subKey,
                        FieldName.FIELD_CLFILE_PROPERTIES);
            }
            else if (!AVAILABLE_FIELD.contains(field)) {
                throw new ScmOperationUnsupportedException(
                        "field can't be modified:fieldName=" + field);
            }
        }
    }

    // value type is string. and can't be null
    public static final Set<String> VALUE_CHECK_STRING_FIELDS = new HashSet<>();
    static {
        VALUE_CHECK_STRING_FIELDS.add(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        VALUE_CHECK_STRING_FIELDS.add(FieldName.FIELD_CLFILE_NAME);
        VALUE_CHECK_STRING_FIELDS.add(FieldName.FIELD_CLFILE_FILE_TITLE);
        VALUE_CHECK_STRING_FIELDS.add(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);

        VALUE_CHECK_STRING_FIELDS.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        VALUE_CHECK_STRING_FIELDS.add(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);

        VALUE_CHECK_STRING_FIELDS.add(FieldName.FIELD_CLFILE_FILE_CLASS_ID);
    }
    
    public static void checkStringFields(BSONObject obj) throws ScmServerException {
        for (String field : VALUE_CHECK_STRING_FIELDS) {
            if (obj.containsField(field)) {
                ScmMetaSourceHelper.checkExistString(obj, field);
            }
        }
    }

    // 文件所有版本需要保持一致的属性
    public static final Set<String> UNIFIED_FIELD = new HashSet<>();
    static {
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_NAME);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_BATCH_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_INNER_USER);
    }
    // external_data 属性内，需要所有版本保持一致的字段
    public static final Set<String> EXTERNAL_DATA_UNIFIED_FIELD = new HashSet<>();
    static {
        EXTERNAL_DATA_UNIFIED_FIELD.add(FieldName.FIELD_CLFILE_FILE_EXT_NAME_BEFORE_ATTACH);
    }

    // 目录文件关系表所映射的文件属性
    public static final Map<String, String> FILE_FIELD_MAP_REL_FIELD = new HashMap<>();
    static {
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_DIRECTORY_ID,
                FieldName.FIELD_CLREL_DIRECTORY_ID);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_NAME, FieldName.FIELD_CLREL_FILENAME);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_ID, FieldName.FIELD_CLREL_FILEID);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_USER, FieldName.FIELD_CLREL_USER);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER,
                FieldName.FIELD_CLREL_UPDATE_USER);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME,
                FieldName.FIELD_CLREL_CREATE_TIME);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME,
                FieldName.FIELD_CLREL_UPDATE_TIME);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                FieldName.FIELD_CLREL_MAJOR_VERSION);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_MINOR_VERSION,
                FieldName.FIELD_CLREL_MINOR_VERSION);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_SIZE,
                FieldName.FIELD_CLREL_FILE_SIZE);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE,
                FieldName.FIELD_CLREL_FILE_MIME_TYPE);
        FILE_FIELD_MAP_REL_FIELD.put(FieldName.FIELD_CLFILE_DELETE_MARKER,
                FieldName.FIELD_CLREL_FILE_DELETE_MARKER);
    }

    // 桶文件关系表所映射的文件属性
    public static final Map<String, String> BUCKET_FILE_REL_FIELD = new HashMap<>();
    static {
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_NAME, FieldName.BucketFile.FILE_NAME);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_ID, FieldName.BucketFile.FILE_ID);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME,
                FieldName.BucketFile.FILE_CREATE_TIME);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_SIZE, FieldName.BucketFile.FILE_SIZE);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_MINOR_VERSION,
                FieldName.BucketFile.FILE_MINOR_VERSION);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_MAJOR_VERSION,
                FieldName.BucketFile.FILE_MAJOR_VERSION);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE,
                FieldName.BucketFile.FILE_MIME_TYPE);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME,
                FieldName.BucketFile.FILE_UPDATE_TIME);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_INNER_USER,
                FieldName.BucketFile.FILE_CREATE_USER);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_FILE_ETAG, FieldName.BucketFile.FILE_ETAG);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_VERSION_SERIAL,
                FieldName.BucketFile.FILE_VERSION_SERIAL);
        BUCKET_FILE_REL_FIELD.put(FieldName.FIELD_CLFILE_DELETE_MARKER,
                FieldName.BucketFile.FILE_DELETE_MARKER);
    }

}
