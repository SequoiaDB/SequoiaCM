package com.sequoiacm.s3.core;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ObjectUri;

public class ObjectMeta {
    public static final String META_KEY_NAME = "Key";
    public static final String META_BUCKET_ID = "BucketId";
    public static final String META_CS_NAME = "CSName";
    public static final String META_CL_NAME = "CLName";
    public static final String META_LOB_ID = "LobId";
    public static final String META_VERSION_ID = "VersionId";
    public static final String META_NO_VERSION_FLAG = "NoVersionFlag";
    public static final String META_LAST_MODIFIED = "LastModified";
    public static final String META_SIZE = "Size";
    public static final String META_ETAG = "Etag";
    public static final String META_DELETE_MARKER = "DeleteMarker";
    public static final String META_CONTENT_TYPE = "ContentType";
    public static final String META_CONTENT_ENCODING = "ContentEncoding";
    public static final String META_CACHE_CONTROL = "CacheControl";
    public static final String META_CONTENT_DISPOSITION = "ContentDisposition";
    public static final String META_EXPIRES = "Expires";
    public static final String META_CONTENT_LANGUAGE = "ContentLanguage";
    public static final String META_LIST = "MetaList";
    public static final String META_PARENTID1 = "ParentId1";
    public static final String META_PARENTID2 = "ParentId2";
    public static final String META_ACLID = "AclID";

    public static final String NULL_VERSION_ID = "null";

    public static final String INDEX_CUR_KEY = ObjectMeta.META_BUCKET_ID + "_"
            + ObjectMeta.META_KEY_NAME;
    public static final String INDEX_HIS_KEY = ObjectMeta.META_BUCKET_ID + "_"
            + ObjectMeta.META_KEY_NAME + "_" + ObjectMeta.META_VERSION_ID;
    public static final String INDEX_CUR_PARENTID1 = ObjectMeta.META_BUCKET_ID + "_"
            + ObjectMeta.META_KEY_NAME + "_" + ObjectMeta.META_PARENTID1;
    public static final String INDEX_CUR_PARENTID2 = ObjectMeta.META_BUCKET_ID + "_"
            + ObjectMeta.META_KEY_NAME + "_" + ObjectMeta.META_PARENTID2;
    private String scmFileId;
    private String key;
    private String bucketName;
    private long versionId;
    private boolean noVersionFlag;
    private long size;
    private long lastModified;
    private String md5;
    private String eTag;
    private Boolean deleteMarker = false;
    private String contentEncoding;
    private String contentType;
    private String cacheControl;
    private String contentDisposition;
    private String expires;
    private String contentLanguage;
    private Map<String, String> metaList = new HashMap<>();
    private boolean isChunkBody;
    private int metaListlength = -1;

    public ObjectMeta() {

    }

    public int getMetaListlength() {
        return metaListlength;
    }

    public String getScmFileId() {
        return scmFileId;
    }

    public void setScmFileId(String scmFileId) {
        this.scmFileId = scmFileId;
    }

    public ObjectMeta(HttpServletRequest req) throws S3ServerException {
        ObjectUri uri = new ObjectUri(req.getRequestURI());
        bucketName = uri.getBucketName();
        key = uri.getObjectName();
        if (key.length() > RestParamDefine.KEY_LENGTH) {
            throw new S3ServerException(S3Error.OBJECT_KEY_TOO_LONG,
                    "ObjectName is too long. objectName:" + key);
        }
        versionId = uri.getVersionId();
        noVersionFlag = uri.isNoVersion();

        String lenHeader = req.getHeader("x-amz-decoded-content-length");
        if (lenHeader != null) {
            isChunkBody = true;
            size = Long.parseLong(lenHeader);
        }
        else {
            isChunkBody = false;
            lenHeader = req.getHeader("content-length");
            if (lenHeader != null) {
                size = Long.parseLong(lenHeader);
            }
        }

        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith(RestParamDefine.PutObjectHeader.X_AMZ_META_PREFIX)) {
                String value = req.getHeader(name);
                metaList.put(name, value);
                metaListlength += (name.length()
                        - RestParamDefine.PutObjectHeader.X_AMZ_META_PREFIX.length());
                metaListlength += value.length();
            }
        }

        cacheControl = req.getHeader(RestParamDefine.PutObjectHeader.CACHE_CONTROL);
        contentDisposition = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_DISPOSITION);
        contentEncoding = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_ENCODING);
        contentType = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_TYPE);
        expires = req.getHeader(RestParamDefine.PutObjectHeader.EXPIRES);
        contentLanguage = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_LANGUAGE);
        md5 = req.getHeader(RestParamDefine.PutObjectHeader.CONTENT_MD5);
    }

    public String getMd5() {
        return md5;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isChunkBody() {
        return isChunkBody;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public long getVersionId() {
        return versionId;
    }

    public void setVersionId(long versionId) {
        this.versionId = versionId;
    }

    public boolean getNoVersionFlag() {
        return noVersionFlag;
    }

    public void setNoVersionFlag(Boolean noVersionFlag) {
        this.noVersionFlag = noVersionFlag;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setEtag(String eTag) {
        this.eTag = eTag;
    }

    public String geteTag() {
        return eTag;
    }

    public void setDeleteMarker(Boolean deleteMarker) {
        this.deleteMarker = deleteMarker;
    }

    public Boolean getDeleteMarker() {
        return deleteMarker;
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

}
