package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OmFileTrafficStatistics {

    @JsonProperty("file_upload_traffic")
    private List<OmStatisticsInfo> uploadTraffics;

    @JsonProperty("file_download_traffic")
    private List<OmStatisticsInfo> downloadTraffics;

    public List<OmStatisticsInfo> getUploadTraffics() {
        return uploadTraffics;
    }

    public void setUploadTraffics(List<OmStatisticsInfo> uploadTraffics) {
        this.uploadTraffics = uploadTraffics;
    }

    public List<OmStatisticsInfo> getDownloadTraffics() {
        return downloadTraffics;
    }

    public void setDownloadTraffics(List<OmStatisticsInfo> downloadTraffics) {
        this.downloadTraffics = downloadTraffics;
    }
}
