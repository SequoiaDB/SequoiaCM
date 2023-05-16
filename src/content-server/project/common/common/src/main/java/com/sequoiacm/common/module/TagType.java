package com.sequoiacm.common.module;

import com.sequoiacm.common.FieldName;

public enum TagType {
    CUSTOM_TAG(FieldName.FIELD_CLFILE_CUSTOM_TAG),
    TAGS(FieldName.FIELD_CLFILE_TAGS);

    private final String fileField;

    TagType(String fileField) {
        this.fileField = fileField;
    }

    public String getFileField() {
        return fileField;
    }

    public static TagType fromFileField(String fileField) {
        for (TagType tagType : TagType.values()) {
            if (tagType.getFileField().equals(fileField)) {
                return tagType;
            }
        }
        return null;
    }
}
