package com.sequoiacm.infrastructure.monitor.model;

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkspaceFlow {
    public static final String UPLOAD = "upload_flow";
    public static final String DOWNLOAD = "download_flow";
    public static final String WS_NAME = "workspace_name";

    @JsonProperty(WS_NAME)
    private String workspaceName;

    @JsonProperty(UPLOAD)
    private AtomicLong upload = new AtomicLong(0);

    @JsonProperty(DOWNLOAD)
    private AtomicLong download = new AtomicLong(0);

    public WorkspaceFlow() {
        workspaceName = "";
    }

    public WorkspaceFlow(String wsName) {
        this.workspaceName = wsName;
    }

    public void addUploadSize(long size) {
        upload.addAndGet(size);
    }

    public void addDownloadSize(long size) {
        download.addAndGet(size);
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public AtomicLong getUpload() {
        return upload;
    }

    public void setUpload(AtomicLong upload) {
        this.upload = upload;
    }

    public AtomicLong getDownload() {
        return download;
    }

    public void setDownload(AtomicLong download) {
        this.download = download;
    }
}
