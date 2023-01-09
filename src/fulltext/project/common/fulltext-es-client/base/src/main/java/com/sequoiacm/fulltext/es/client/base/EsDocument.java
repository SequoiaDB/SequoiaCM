package com.sequoiacm.fulltext.es.client.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.fulltext.common.FulltextDocDefine;

public class EsDocument {
    @JsonProperty(FulltextDocDefine.FIELD_FILE_ID)
    private String fileId;
    @JsonProperty(FulltextDocDefine.FIELD_FILE_CONTENT)
    private String content;
    @JsonProperty(FulltextDocDefine.FIELD_FILE_VERSION)
    private String fileVersion;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(String fileVersion) {
        this.fileVersion = fileVersion;
    }

    @Override
    public String toString() {
        return "FulltextIdxDocument [fileId=" + fileId + ", fileVersion=" + fileVersion + "]";
    }

}
