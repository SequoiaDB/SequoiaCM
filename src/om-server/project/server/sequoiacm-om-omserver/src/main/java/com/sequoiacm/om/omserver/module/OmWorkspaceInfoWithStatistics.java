package com.sequoiacm.om.omserver.module;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmWorkspaceInfoWithStatistics extends OmWorkspaceDetail {
    @JsonProperty("file_count")
    private long fileCount;

    @JsonProperty("directory_count")
    private long directoryCount;

    @JsonProperty("batch_count")
    private long batchCount;

    public long getFileCount() {
        return fileCount;
    }

    public void setFileCount(long fileCount) {
        this.fileCount = fileCount;
    }

    public long getDirectoryCount() {
        return directoryCount;
    }

    public void setDirectoryCount(long directoryCount) {
        this.directoryCount = directoryCount;
    }

    public long getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(long batchCount) {
        this.batchCount = batchCount;
    }
}
