package com.sequoiacm.s3.core;

public class S3ObjectMeta extends S3BasicObjectMeta {
    private String etag;
    private String versionId;
    private long lastModified;

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getEtag() {
        return etag;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getLastModified() {
        return lastModified;
    }

    @Override
    public String toString() {
        return "S3ObjectMeta{" + "etag='" + etag + '\'' + ", versionId='" + versionId + '\''
                + ", lastModified=" + lastModified + "super=" + super.toString() + '}';
    }
}
