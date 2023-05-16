package com.sequoiacm.om.omserver.module.tag;

public enum OmTagType {

    TAGS("tags"),
    CUSTOM_TAG("custom_tag");

    private String type;

    OmTagType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static OmTagType getType(String type) {
        OmTagType[] var1 = values();
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            OmTagType tagType = var1[var3];
            if (tagType.getType().equals(type)) {
                return tagType;
            }
        }

        throw new IllegalArgumentException("Unknown tag type: " + type);
    }
}
