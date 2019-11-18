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

    @JsonProperty("file_upload_traffic")
    private List<OmStatisticsInfo> fileUploadTraffic;

    @JsonProperty("file_download_traffic")
    private List<OmStatisticsInfo> fileDownloadTraffic;

    @JsonProperty("file_count_delta")
    private List<OmStatisticsInfo> fileCountDelta;

    @JsonProperty("file_size_delta")
    private List<OmStatisticsInfo> fileSizeDelta;

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

    public List<OmStatisticsInfo> getFileUploadTraffic() {
        return fileUploadTraffic;
    }

    public void setFileUploadTraffic(List<OmStatisticsInfo> fileUploadTraffic) {
        this.fileUploadTraffic = fileUploadTraffic;
    }

    public List<OmStatisticsInfo> getFileDownloadTraffic() {
        return fileDownloadTraffic;
    }

    public void setFileDownloadTraffic(List<OmStatisticsInfo> fileDownloadTraffic) {
        this.fileDownloadTraffic = fileDownloadTraffic;
    }

    public List<OmStatisticsInfo> getFileCountDelta() {
        return fileCountDelta;
    }

    public void setFileCountDelta(List<OmStatisticsInfo> fileCountDelta) {
        this.fileCountDelta = fileCountDelta;
    }

    public List<OmStatisticsInfo> getFileSizeDelta() {
        return fileSizeDelta;
    }

    public void setFileSizeDelta(List<OmStatisticsInfo> fileSizeDelta) {
        this.fileSizeDelta = fileSizeDelta;
    }
}
