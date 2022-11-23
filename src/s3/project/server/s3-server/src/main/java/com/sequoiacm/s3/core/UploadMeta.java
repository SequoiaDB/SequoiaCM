package com.sequoiacm.s3.core;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class UploadMeta {
    public static final String META_KEY_NAME = "key";
    public static final String META_BUCKET_ID = "bucket_id";
    public static final String META_SITE_ID = "site_id";
    public static final String META_SITE_TYPE = "site_type";
    public static final String META_DATA_ID = "data_id";
    public static final String META_WORKSPACE = "workspace";
    public static final String META_UPLOAD_ID = "upload_id";
    public static final String META_STATUS = "upload_status";
    public static final String META_LAST_MODIFY_TIME = "last_modified_time";
    public static final String META_CONTENT_TYPE = "content_type";
    public static final String META_CONTENT_ENCODING = "content_encoding";
    public static final String META_CACHE_CONTROL = "cache_control";
    public static final String META_CONTENT_DISPOSITION = "content_disposition";
    public static final String META_EXPIRES = "expires";
    public static final String META_CONTENT_LANGUAGE = "content_language";
    public static final String META_LIST = "meta_list";
    public static final String META_WS_VERSION = "ws_version";

    private String key;
    private long bucketId;
    private String dataId;
    private int siteId;
    private String siteType;
    private long uploadId;
    private int uploadStatus;
    private long lastModified;
    private String contentEncoding;
    private String contentType;
    private String cacheControl;
    private String contentDisposition;
    private String expires;
    private String contentLanguage;
    private Map<String, String> metaList = new HashMap<>();
    private int metaListLength = 0;
    private String wsName;
    private int wsVersion;

    public UploadMeta() {
    }

    public UploadMeta(BSONObject record) {
        this.bucketId = (long) record.get(UploadMeta.META_BUCKET_ID);
        this.key = record.get(UploadMeta.META_KEY_NAME).toString();
        this.uploadId = (long) record.get(UploadMeta.META_UPLOAD_ID);
        this.lastModified = (long) record.get(UploadMeta.META_LAST_MODIFY_TIME);
        this.uploadStatus = (int) record.get(UploadMeta.META_STATUS);
        this.siteId = (int) record.get(UploadMeta.META_SITE_ID);
        if (record.get(UploadMeta.META_SITE_TYPE) != null) {
            this.siteType = record.get(UploadMeta.META_SITE_TYPE).toString();
        }
        if (record.get(UploadMeta.META_DATA_ID) != null) {
            this.dataId = record.get(UploadMeta.META_DATA_ID).toString();
        }

        if (record.get(UploadMeta.META_WORKSPACE) != null) {
            this.wsName = record.get(UploadMeta.META_WORKSPACE).toString();
        }

        if (record.get(UploadMeta.META_CACHE_CONTROL) != null) {
            this.cacheControl = record.get(UploadMeta.META_CACHE_CONTROL).toString();
        }
        if (record.get(UploadMeta.META_CONTENT_DISPOSITION) != null) {
            this.contentDisposition = record.get(UploadMeta.META_CONTENT_DISPOSITION).toString();
        }
        if (record.get(UploadMeta.META_CONTENT_ENCODING) != null) {
            this.contentEncoding = record.get(UploadMeta.META_CONTENT_ENCODING).toString();
        }
        if (record.get(UploadMeta.META_CONTENT_LANGUAGE) != null) {
            this.contentLanguage = record.get(UploadMeta.META_CONTENT_LANGUAGE).toString();
        }
        if (record.get(UploadMeta.META_CONTENT_TYPE) != null) {
            this.contentType = record.get(UploadMeta.META_CONTENT_TYPE).toString();
        }
        if (record.get(UploadMeta.META_EXPIRES) != null) {
            this.expires = record.get(UploadMeta.META_EXPIRES).toString();
        }
        if (record.get(UploadMeta.META_LIST) != null) {
            this.metaList = ((BSONObject) record.get(UploadMeta.META_LIST)).toMap();
        }
        if (record.get(UploadMeta.META_WS_VERSION) != null) {
            this.wsVersion = (int) record.get(UploadMeta.META_WS_VERSION);
        }
        else {
            this.wsVersion = 1;
        }
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setUploadId(long uploadId) {
        this.uploadId = uploadId;
    }

    public long getUploadId() {
        return uploadId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public String getWsName() {
        return wsName;
    }

    public void setUploadStatus(int uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public int getUploadStatus() {
        return uploadStatus;
    }

    public void setLastModified(long lastModifiedTime) {
        this.lastModified = lastModifiedTime;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getExpires() {
        return expires;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setMetaList(Map<String, String> metaList) {
        this.metaList = metaList;
    }

    public Map<String, String> getMetaList() {
        return metaList;
    }

    public int getMetaListlength() {
        return metaListLength;
    }

    public void setWsVersion(int wsVersion) {
        this.wsVersion = wsVersion;
    }

    public int getWsVersion() {
        return wsVersion;
    }

    public UploadMeta(HttpServletRequest req) throws S3ServerException {
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith(RestParamDefine.PutObjectHeader.X_AMZ_META_PREFIX)) {
                String value = req.getHeader(name);
                metaList.put(name, value);
                metaListLength += (name.length()
                        - RestParamDefine.PutObjectHeader.X_AMZ_META_PREFIX.length());
                metaListLength += value.length();
            }
        }

        cacheControl = req.getHeader(RestParamDefine.PutObjectHeader.CACHE_CONTROL);
        contentDisposition = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_DISPOSITION);
        contentEncoding = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_ENCODING);
        contentType = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_TYPE);
        expires = req.getHeader(RestParamDefine.PutObjectHeader.EXPIRES);
        contentLanguage = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_LANGUAGE);
    }

    @Override
    public String toString() {
        return "UploadMeta{key='" + key + "'" + ", bucketId=" + bucketId + ", uploadId=" + uploadId
                + ", wsName='" + wsName + "'" + ", siteId=" + siteId + ", siteType='" + siteType
                + "'" + ", uploadStatus=" + uploadStatus + ", uploadTime=" + lastModified
                + ", contentEncoding='" + contentEncoding + '\'' + ", contentType='" + contentType
                + '\'' + ", cacheControl='" + cacheControl + '\'' + ", contentDisposition='"
                + contentDisposition + '\'' + ", expires='" + expires + '\'' + ", contentLanguage='"
                + contentLanguage + '\'' + ", metaList=" + metaList + ", wsVersoin=" + wsVersion + '}';
    }
}
