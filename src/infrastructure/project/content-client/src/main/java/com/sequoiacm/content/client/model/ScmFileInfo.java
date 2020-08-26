package com.sequoiacm.content.client.model;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.infrastructure.common.BsonUtils;

public class ScmFileInfo {
    private String id;
    private String name;
    private String createUser;
    private long createTime;
    private MimeType mimeType;
    private int majorVersion;
    private int minorVersion;
    private BSONObject externalData;
    private List<Integer> sites;
    private long fileSize;
    private BSONObject bson;

    public ScmFileInfo() {
    }

    public ScmFileInfo(BSONObject file) {
        bson = file;
        id = BsonUtils.getStringChecked(file, FieldName.FIELD_CLFILE_ID);
        majorVersion = BsonUtils.getIntegerChecked(file, FieldName.FIELD_CLFILE_MAJOR_VERSION);
        minorVersion = BsonUtils.getIntegerChecked(file, FieldName.FIELD_CLFILE_MINOR_VERSION);
        externalData = BsonUtils.getBSON(file, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        String mime = BsonUtils.getString(file, FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
        if (mime != null) {
            mimeType = MimeType.get(mime);
        }
        BasicBSONList sitesBsons = BsonUtils.getArrayChecked(file,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        sites = new ArrayList<Integer>();
        for (Object siteBson : sitesBsons) {
            sites.add(BsonUtils.getIntegerChecked((BSONObject) siteBson,
                    FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID));
        }
        name = BsonUtils.getStringChecked(file, FieldName.FIELD_CLFILE_NAME);
        createUser = BsonUtils.getStringChecked(file, FieldName.FIELD_CLFILE_INNER_USER);
        createTime = BsonUtils.getNumberChecked(file, FieldName.FIELD_CLFILE_INNER_CREATE_TIME)
                .longValue();
        fileSize = BsonUtils.getNumberChecked(file, FieldName.FIELD_CLFILE_FILE_SIZE).longValue();
    }

    public BSONObject getBson() {
        return bson;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public BSONObject getExternalData() {
        return externalData;
    }

    public void setExternalData(BSONObject externalData) {
        this.externalData = externalData;
    }

    public List<Integer> getSites() {
        return sites;
    }

    public void setSites(List<Integer> sites) {
        this.sites = sites;
    }

    @Override
    public String toString() {
        return "ScmFileInfo [id=" + id + ", mimeType=" + mimeType + ", majorVersion=" + majorVersion
                + ", minorVersion=" + minorVersion + ", externalData=" + externalData + ", sites="
                + sites + "]";
    }

}
