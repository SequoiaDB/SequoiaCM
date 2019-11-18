package com.sequoiacm.om.omserver.module;

import java.util.List;

public class OmFileTrafficStatistics {
    private List<OmStatisticsInfo> uploadTraffics;
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
