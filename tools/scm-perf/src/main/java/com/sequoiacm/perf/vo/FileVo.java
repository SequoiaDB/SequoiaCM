package com.sequoiacm.perf.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileVo {

    @JsonProperty("file")
    private FileId fileId;

    public String getFileId() {
        return fileId.getFileId();
    }
}

class FileId {
    @JsonProperty("id")
    private String fileId;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
