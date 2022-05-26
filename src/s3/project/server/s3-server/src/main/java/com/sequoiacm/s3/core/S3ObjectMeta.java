package com.sequoiacm.s3.core;

public class S3ObjectMeta extends S3BasicObjectMeta {
    private String etag;
    private String versionId;
    private long lastModified;
    private boolean isDeleteMarker = false;
    private String user;

    public S3ObjectMeta() {
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

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

    public void setDeleteMarker(boolean deleteMarker) {
        isDeleteMarker = deleteMarker;
    }

    public boolean isDeleteMarker() {
        return isDeleteMarker;
    }

    @Override
    public String toString() {
        return "S3ObjectMeta{" + "etag='" + etag + '\'' + ", versionId='" + versionId + '\''
                + ", lastModified=" + lastModified + ", isDeleteMarker=" + isDeleteMarker
                + ", deleteMarker=" + isDeleteMarker() + ", key='" + getKey() + '\'' + ", bucket='"
                + getBucket() + '\'' + ", contentEncoding='" + getContentEncoding() + '\''
                + ", contentType='" + getContentType() + '\'' + ", cacheControl='"
                + getCacheControl() + '\'' + ", contentDisposition='" + getContentDisposition()
                + '\'' + ", expires='" + getExpires() + '\'' + ", contentLanguage='"
                + getContentLanguage() + '\'' + ", size=" + getSize() + ", metaList="
                + getMetaList() + ", metaListLength=" + getMetaListLength() + '}';
    }
}
