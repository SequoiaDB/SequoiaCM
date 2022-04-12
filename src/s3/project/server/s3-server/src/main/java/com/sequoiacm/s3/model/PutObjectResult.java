package com.sequoiacm.s3.model;

public class PutObjectResult {
    private String  eTag;
    private String  versionId;

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String geteTag() {
        return eTag;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getVersionId() {
        return versionId;
    }
}
