package com.sequoiacm.s3.core;

import java.util.HashMap;
import java.util.Map;

public class S3BasicObjectMeta {
    private String key;
    private String bucket;
    private String contentEncoding;
    private String contentType;
    private String cacheControl;
    private String contentDisposition;
    private String expires;
    private String contentLanguage;
    private long size;
    private Map<String, String> metaList = new HashMap<>();
    private Map<String, String> tagging = new HashMap<>();

    public S3BasicObjectMeta() {
    }

    public S3BasicObjectMeta(S3BasicObjectMeta other) {
        key = other.key;
        bucket = other.bucket;
        contentEncoding = other.contentEncoding;
        contentType = other.contentType;
        cacheControl = other.cacheControl;
        contentDisposition = other.contentDisposition;
        expires = other.expires;
        contentLanguage = other.contentLanguage;
        size = other.size;
        metaList = new HashMap<>(other.metaList);
        this.tagging = other.tagging;
    }

    @Override
    public String toString() {
        return "S3BasicObjectMeta{" + "key='" + key + '\'' + ", bucket='" + bucket + '\''
                + ", contentEncoding='" + contentEncoding + '\'' + ", contentType='" + contentType
                + '\'' + ", cacheControl='" + cacheControl + '\'' + ", contentDisposition='"
                + contentDisposition + '\'' + ", expires='" + expires + '\'' + ", contentLanguage='"
                + contentLanguage + '\'' + ", size=" + size + ", metaList=" + metaList
                + ", tagging=" + tagging + '}';
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Map<String, String> getMetaList() {
        return metaList;
    }

    public void setMetaList(Map<String, String> metaList) {
        this.metaList = metaList;
    }

    public int getMetaListLength() {
        if (metaList == null) {
            return 0;
        }
        int ret = 0;
        for (Map.Entry<String, String> entry : metaList.entrySet()) {
            ret += entry.getKey().length();
            ret += entry.getValue().length();
        }
        return ret;
    }

    public Map<String, String> getTagging() {
        return tagging;
    }

    public void setTagging(Map<String, String> tagging) {
        this.tagging = tagging;
    }
}
