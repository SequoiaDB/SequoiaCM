package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.exception.ScmServerException;

import java.util.HashSet;
import java.util.Set;

public class UpdaterKeyDefine {
    public static final String REMOVE_TAG = CommonDefine.RestArg.UPDATE_INFO_REMOVE_TAG;
    public static final String REMOVE_CUSTOM_TAG = CommonDefine.RestArg.UPDATE_INFO_REMOVE_CUSTOM_TAG;

    public static final String ADD_TAG = CommonDefine.RestArg.UPDATE_INFO_ADD_TAG;
    public static final String ADD_CUSTOM_TAG = CommonDefine.RestArg.UPDATE_INFO_ADD_CUSTOM_TAG;

    private static final Set<String> AVAILABLE_UPDATER_KEY = new HashSet<>();
    static {
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_NAME);
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_FILE_TITLE);
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);

        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        AVAILABLE_UPDATER_KEY.add(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);

        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_PROPERTIES);
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_TAGS);
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        AVAILABLE_UPDATER_KEY.add(FieldName.FIELD_CLFILE_CUSTOM_TAG);
        AVAILABLE_UPDATER_KEY.add(ADD_TAG);
        AVAILABLE_UPDATER_KEY.add(ADD_CUSTOM_TAG);
        AVAILABLE_UPDATER_KEY.add(REMOVE_TAG);
        AVAILABLE_UPDATER_KEY.add(REMOVE_CUSTOM_TAG);
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
            else if (!AVAILABLE_UPDATER_KEY.contains(field)) {
                throw new ScmOperationUnsupportedException(
                        "field can't be modified:fieldName=" + field);
            }
        }
    }
}
