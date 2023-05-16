package com.sequoiacm.tools.tag.common;

public class WsBasicInfo {
    private final String tagLibCsName;
    private final String tagLibClName;
    private String name;
    private String tagLibTableName;

    public WsBasicInfo(String name, String tagLibTableName) {
        this.name = name;
        this.tagLibTableName = tagLibTableName;
        String[] csClArr = tagLibTableName.split("\\.");
        if (csClArr.length != 2) {
            throw new IllegalArgumentException("tagLibTableName is invalid: ws=" + name
                    + ", tagLibTableName=" + tagLibTableName);
        }

        tagLibCsName = csClArr[0];
        tagLibClName = csClArr[1];
    }

    public String getName() {
        return name;
    }

    public String getTagLibTableFullName() {
        return tagLibTableName;
    }

    public String getTagLibTableCsName() {
        return tagLibCsName;
    }

    public String getTagLibTableClName() {
        return tagLibClName;
    }
}
