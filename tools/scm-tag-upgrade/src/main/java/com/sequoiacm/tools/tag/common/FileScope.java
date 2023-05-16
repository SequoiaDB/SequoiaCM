package com.sequoiacm.tools.tag.common;

public enum FileScope {

    CURRENT("FILE"),
    HISTORY("FILE_HISTORY");

    private String fileClName;

    FileScope(String fileClName) {
        this.fileClName = fileClName;
    }

    public String getFileClName() {
        return fileClName;
    }
}
