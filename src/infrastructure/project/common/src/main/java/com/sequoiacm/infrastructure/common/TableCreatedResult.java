package com.sequoiacm.infrastructure.common;

public class TableCreatedResult {
    private String csName;
    private boolean isInExtraCs;
    private String clName;

    public TableCreatedResult(String csName, boolean isInExtraCs, String clName) {
        this.csName = csName;
        this.isInExtraCs = isInExtraCs;
        this.clName = clName;
    }

    public String getCsName() {
        return csName;
    }

    public boolean isInExtraCs() {
        return isInExtraCs;
    }

    public String getFullClName() {
        return csName + "." + clName;
    }
}
