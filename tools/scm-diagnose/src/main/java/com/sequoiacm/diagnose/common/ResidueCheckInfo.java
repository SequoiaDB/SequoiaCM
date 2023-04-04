package com.sequoiacm.diagnose.common;

import com.sequoiacm.diagnose.progress.ResidueProgress;

public class ResidueCheckInfo {
    private String workspace;
    private String site;
    private ResidueProgress progress;
    private String dataTable;
    private String dataIdFilePath;

    public ResidueCheckInfo(String workspace, String site, ResidueProgress progress) {
        this.workspace = workspace;
        this.site = site;
        this.progress = progress;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getSite() {
        return site;
    }

    public ResidueProgress getProgress() {
        return progress;
    }

    public void setDataTable(String dataTable) {
        this.dataTable = dataTable;
    }

    public void setDataIdFilePath(String dataIdFilePath) {
        this.dataIdFilePath = dataIdFilePath;
    }

    public String getDataIdFilePath() {
        return dataIdFilePath;
    }

    public String getDataTable() {
        return dataTable;
    }
}
