package com.sequoiacm.fulltext.server.es;

public class EsDocument {
    private String fileId;
    private String content;
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
