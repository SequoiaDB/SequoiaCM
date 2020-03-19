package com.sequoiacm.s3.remote;

import java.util.Map;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.s3.common.S3CommonDefine;

public class ScmFileInfo {
    private String dirId;
    private String id;
    private String name;
    private String user;
    private long size;
    private long updateTime;
    private String mimeType;
    private Map<String, String> customMetaMetaList;
    private String customMetaEnconde;
    private String customMetaCacheControl;
    private String customMetaContentLanguage;
    private String customMetaEtag;
    private String customMetaExpires;
    private String customMetaContentDisposition;

    public ScmFileInfo(BSONObject node) {
        ObjectMapper objMapper = new ObjectMapper();
        init(objMapper.valueToTree(node.toMap()));
    }

    public ScmFileInfo(JsonNode node) {
        init(node);
    }

    public void setDirId(String dirId) {
        this.dirId = dirId;
    }

    public String getDirId() {
        return dirId;
    }

    @SuppressWarnings("unchecked")
    private void init(JsonNode node) {
        if (node.size() == 1 && node.has(CommonDefine.RestArg.FILE_RESP_FILE_INFO)) {
            node = node.get(CommonDefine.RestArg.FILE_RESP_FILE_INFO);
        }
        this.setId(node.get(FieldName.FIELD_CLFILE_ID).asText());
        this.setName(node.get(FieldName.FIELD_CLFILE_NAME).asText());
        this.setMimeType(node.get(FieldName.FIELD_CLFILE_FILE_MIME_TYPE).asText());
        this.setUser(node.get(FieldName.FIELD_CLFILE_INNER_USER).asText());
        this.setUpdateTime(node.get(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME).asLong());
        this.setSize(node.get(FieldName.FIELD_CLFILE_FILE_SIZE).asLong());
        this.setDirId(node.get(FieldName.FIELD_CLFILE_DIRECTORY_ID).asText());

        JsonNode ele = node.get(FieldName.FIELD_CLFILE_PROPERTIES);
        if (ele != null) {
            JsonNode customProp = ele.get(S3CommonDefine.S3_CUSTOM_META_CACHE_CONTROL);
            if (customProp != null) {
                this.setCustomMetaCacheControl(customProp.asText());
            }
            customProp = ele.get(S3CommonDefine.S3_CUSTOM_META_CONENT_ENCODE);
            if (customProp != null) {
                this.setCustomMetaEnconde(customProp.asText());
            }

            customProp = ele.get(S3CommonDefine.S3_CUSTOM_META_CONTENT_LANGUAGE);
            if (customProp != null) {
                this.setCustomMetaContentLanguage(customProp.asText());
            }

            customProp = ele.get(S3CommonDefine.S3_CUSTOM_META_ETAG);
            if (customProp != null) {
                this.setCustomMetaEtag(customProp.asText());
            }

            customProp = ele.get(S3CommonDefine.S3_CUSTOM_META_EXPIRES);
            if (customProp != null) {
                this.setCustomMetaExpires(customProp.asText());
            }

            customProp = ele.get(S3CommonDefine.S3_CUSTOM_META_CONTENT_DISPOSITION);
            if (customProp != null) {
                this.setCustomMetaContentDisposition(customProp.asText());
            }

            customProp = ele.get(S3CommonDefine.S3_CUSTOM_META_META_LIST);
            if (customProp != null) {
                BSONObject metaJson = (BSONObject) JSON.parse(customProp.asText());
                this.setCustomMetaMetaList(metaJson.toMap());
            }
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getCustomMetaContentDisposition() {
        return customMetaContentDisposition;
    }

    public void setCustomMetaContentDisposition(String customMetaContentDisposition) {
        this.customMetaContentDisposition = customMetaContentDisposition;
    }

    public String getCustomMetaCacheControl() {
        return customMetaCacheControl;
    }

    public void setCustomMetaCacheControl(String customMetaCacheControl) {
        this.customMetaCacheControl = customMetaCacheControl;
    }

    public String getCustomMetaContentLanguage() {
        return customMetaContentLanguage;
    }

    public void setCustomMetaContentLanguage(String customMetaContentLanguage) {
        this.customMetaContentLanguage = customMetaContentLanguage;
    }

    public String getCustomMetaEnconde() {
        return customMetaEnconde;
    }

    public void setCustomMetaEnconde(String customMetaEnconde) {
        this.customMetaEnconde = customMetaEnconde;
    }

    public String getCustomMetaEtag() {
        return customMetaEtag;
    }

    public String getCustomMetaExpires() {
        return customMetaExpires;
    }

    public Map<String, String> getCustomMetaMetaList() {
        return customMetaMetaList;
    }

    public void setCustomMetaEtag(String customMetaEtag) {
        this.customMetaEtag = customMetaEtag;
    }

    public void setCustomMetaExpires(String customMetaExpires) {
        this.customMetaExpires = customMetaExpires;
    }

    public void setCustomMetaMetaList(Map<String, String> customMetaMetaList) {
        this.customMetaMetaList = customMetaMetaList;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getSize() {
        return size;
    }

    public String getUser() {
        return user;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
