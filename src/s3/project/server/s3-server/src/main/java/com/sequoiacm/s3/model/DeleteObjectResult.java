package com.sequoiacm.s3.model;

public class DeleteObjectResult {
    private boolean isDeleteMarker;
    private String versionId;

    public boolean isDeleteMarker() {
        return isDeleteMarker;
    }

    public void setDeleteMarker(boolean deleteMarker) {
        isDeleteMarker = deleteMarker;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}
